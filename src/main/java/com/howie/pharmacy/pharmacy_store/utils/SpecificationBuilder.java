package com.howie.pharmacy.pharmacy_store.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest.FilterCriteria;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest.FilterOp;
import com.howie.pharmacy.pharmacy_store.dto.common.PagerRequest.SortOrder;
import com.howie.pharmacy.pharmacy_store.exception.AppExceptions.BadRequestException;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Builds JPA specifications/pageables from a {@link PagerRequest}, restricted
 * to a per-entity field whitelist.
 */
public final class SpecificationBuilder {

    /**
     * Maps a client-facing key to an entity attribute path (dot-separated) with an
     * optional default operator.
     */
    public record Field(String path, FilterOp defaultOp) {
        public static Field of(String path) {
            return new Field(path, null);
        }

        public static Field of(String path, FilterOp defaultOp) {
            return new Field(path, defaultOp);
        }
    }

    private SpecificationBuilder() {
    }

    public static <T> Page<T> page(JpaSpecificationExecutor<T> repository, PagerRequest pager,
            Map<String, Field> fields, Sort defaultSort, Specification<T> extra) {
        PagerRequest request = pager != null ? pager : new PagerRequest();
        Specification<T> spec = SpecificationBuilder.<T>build(request, fields);
        if (extra != null) {
            spec = spec.and(extra);
        }
        return repository.findAll(spec, pageable(request, fields, defaultSort));
    }

    public static Pageable pageable(PagerRequest pager, Map<String, Field> fields, Sort defaultSort) {
        int pageNo = Math.max(pager.getPageNo(), 0);
        int pageSize = Math.min(Math.max(pager.getPageSize(), 1), PagerRequest.MAX_PAGE_SIZE);

        List<Sort.Order> orders = new ArrayList<>();
        if (pager.getSort() != null) {
            for (SortOrder s : pager.getSort()) {
                Field field = resolve(fields, s.getField());
                Sort.Direction direction = "DESC".equalsIgnoreCase(s.getDirection())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                orders.add(new Sort.Order(direction, field.path()));
            }
        }
        Sort sort = orders.isEmpty() ? defaultSort : Sort.by(orders);
        return PageRequest.of(pageNo, pageSize, sort);
    }

    public static <T> Specification<T> build(PagerRequest pager, Map<String, Field> fields) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (pager.getFilters() != null) {
                for (FilterCriteria f : pager.getFilters()) {
                    Field field = resolve(fields, f.getKey());
                    Path<?> path = path(root, field.path());
                    predicates.add(filterPredicate(cb, path, f, field));
                }
            }

            String keyword = pager.getSearchValue();
            if (keyword != null && !keyword.isBlank() && pager.getSearchKeys() != null
                    && !pager.getSearchKeys().isEmpty()) {
                String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                List<Predicate> any = new ArrayList<>();
                for (String key : pager.getSearchKeys()) {
                    Expression<String> expr = path(root, resolve(fields, key).path()).as(String.class);
                    any.add(cb.like(cb.lower(expr), pattern, '\\'));
                }
                predicates.add(cb.or(any.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Predicate filterPredicate(jakarta.persistence.criteria.CriteriaBuilder cb, Path<?> path,
            FilterCriteria f, Field field) {
        List<Object> values = toList(f.getValue());
        if (values.isEmpty()) {
            throw new BadRequestException("Filter '" + f.getKey() + "' requires a value");
        }
        FilterOp op = f.getOp() != null ? f.getOp()
                : field.defaultOp() != null ? field.defaultOp()
                        : values.size() > 1 ? FilterOp.IN : FilterOp.EQ;
        Class<?> type = path.getJavaType();

        switch (op) {
            case EQ:
                return cb.equal(path, convert(values.get(0), type, f.getKey()));
            case IN: {
                List<Object> converted = new ArrayList<>();
                for (Object v : values) {
                    converted.add(convert(v, type, f.getKey()));
                }
                return path.in(converted);
            }
            case BETWEEN: {
                if (values.size() != 2) {
                    throw new BadRequestException("Filter '" + f.getKey() + "' BETWEEN requires exactly 2 values");
                }
                Comparable from = (Comparable) convert(values.get(0), type, f.getKey());
                Comparable to = (Comparable) convert(values.get(1), type, f.getKey());
                return cb.between((Expression<Comparable>) path, from, to);
            }
            case GTE:
                return cb.greaterThanOrEqualTo((Expression<Comparable>) path,
                        (Comparable) convert(values.get(0), type, f.getKey()));
            case LTE:
                return cb.lessThanOrEqualTo((Expression<Comparable>) path,
                        (Comparable) convert(values.get(0), type, f.getKey()));
            case LIKE:
                return cb.like(cb.lower(path.as(String.class)),
                        "%" + escapeLike(String.valueOf(values.get(0)).toLowerCase()) + "%", '\\');
            default:
                throw new BadRequestException("Unsupported operator " + op);
        }
    }

    private static Field resolve(Map<String, Field> fields, String key) {
        Field field = key == null ? null : fields.get(key);
        if (field == null) {
            throw new BadRequestException("Unsupported field: " + key);
        }
        return field;
    }

    private static Path<?> path(jakarta.persistence.criteria.Root<?> root, String dotted) {
        Path<?> p = root;
        for (String part : dotted.split("\\.")) {
            p = p.get(part);
        }
        return p;
    }

    private static List<Object> toList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> c) {
            return new ArrayList<>(c);
        }
        return List.of(value);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object convert(Object value, Class<?> type, String key) {
        try {
            if (value == null || type.isInstance(value)) {
                return value;
            }
            if (value instanceof Number n) {
                if (type == BigDecimal.class) {
                    return new BigDecimal(n.toString());
                } else if (type == Integer.class || type == int.class) {
                    return n.intValue();
                } else if (type == Long.class || type == long.class) {
                    return n.longValue();
                } else if (type == Double.class || type == double.class) {
                    return n.doubleValue();
                } else if (type == Float.class || type == float.class) {
                    return n.floatValue();
                }
            }
            String s = String.valueOf(value);
            if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, s.toUpperCase());
            } else if (type == LocalDateTime.class) {
                return LocalDateTime.parse(s);
            } else if (type == Boolean.class || type == boolean.class) {
                return Boolean.parseBoolean(s);
            } else if (type == BigDecimal.class) {
                return new BigDecimal(s);
            } else if (type == Integer.class || type == int.class) {
                return Integer.valueOf(s);
            } else if (type == Long.class || type == long.class) {
                return Long.valueOf(s);
            } else if (type == String.class) {
                return s;
            }
        } catch (RuntimeException e) {
            throw new BadRequestException("Invalid value for filter '" + key + "'");
        }
        throw new BadRequestException("Unsupported filter type for '" + key + "'");
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
