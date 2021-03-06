package org.apereo.cas.adaptors.redis.services;

import org.apereo.cas.services.AbstractServiceRegistry;
import org.apereo.cas.services.RegisteredService;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the service registry interface which stores the services in a redis instance.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@ToString
public class RedisServiceRegistry extends AbstractServiceRegistry {

    private static final String CAS_SERVICE_PREFIX = RegisteredService.class.getSimpleName() + ':';

    private final RedisTemplate<String, RegisteredService> template;

    public RedisServiceRegistry(final ApplicationEventPublisher eventPublisher, final RedisTemplate<String, RegisteredService> template) {
        super(eventPublisher);
        this.template = template;
    }

    @Override
    public RegisteredService save(final RegisteredService rs) {
        try {
            val redisKey = getRegisteredServiceRedisKey(rs);
            this.template.boundValueOps(redisKey).set(rs);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return rs;
    }

    @Override
    public boolean delete(final RegisteredService registeredService) {
        try {
            val redisKey = getRegisteredServiceRedisKey(registeredService);
            this.template.delete(redisKey);
            return true;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public long size() {
        try {
            return getRegisteredServiceKeys().size();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public Collection<RegisteredService> load() {
        try {
            return getRegisteredServiceKeys()
                .stream()
                .map(redisKey -> this.template.boundValueOps(redisKey).get())
                .filter(Objects::nonNull).collect(Collectors.toList());
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public RegisteredService findServiceById(final long id) {
        try {
            val redisKey = getRegisteredServiceRedisKey(id);
            return this.template.boundValueOps(redisKey).get();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public RegisteredService findServiceById(final String id) {
        return load().stream().filter(r -> r.matches(id)).findFirst().orElse(null);
    }

    private static String getRegisteredServiceRedisKey(final RegisteredService registeredService) {
        return getRegisteredServiceRedisKey(registeredService.getId());
    }

    private static String getRegisteredServiceRedisKey(final long id) {
        return CAS_SERVICE_PREFIX + id;
    }

    private static String getPatternRegisteredServiceRedisKey() {
        return CAS_SERVICE_PREFIX + '*';
    }

    private Set<String> getRegisteredServiceKeys() {
        return this.template.keys(getPatternRegisteredServiceRedisKey());
    }
}
