/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.ipc.cache.php;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.google.inject.Inject;

import de.cosmocode.commons.reflect.Reflection;
import de.cosmocode.palava.core.Registry;
import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import de.cosmocode.palava.ipc.IpcCommand;
import de.cosmocode.palava.ipc.IpcConnection;
import de.cosmocode.palava.ipc.cache.ComplexCacheAnnotation;
import de.cosmocode.palava.ipc.json.custom.CustomPostCallEvent;
import de.cosmocode.palava.ipc.json.custom.CustomProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a flag to the response, that the command may be cached.
 *
 * @author Tobias Sarnowski
 */
final class CachedWeaver implements CustomPostCallEvent, Initializable {
    
    private static final Logger LOG = LoggerFactory.getLogger(CachedWeaver.class);

    // for php
    private static final String CACHED_KEY = "CACHED";

    private final Registry registry;

    @Inject
    CachedWeaver(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void initialize() throws LifecycleException {
        registry.register(CustomPostCallEvent.class, this);
    }

    @Override
    public void eventPostCall(Map<String, Object> request, Map<String, Object> response, IpcConnection connection) {
        // get the command class manually
        final String cmd = String.class.cast(request.get(CustomProtocol.COMMAND));
        final Class<? extends IpcCommand> command;
        
        try {
            command = Reflection.forName(cmd).asSubclass(IpcCommand.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot retrieve the command " + cmd + " as class", e);
        }

        // weave the "CACHED" constant into the meta informations
        response.put(CACHED_KEY, isCacheable(command));
        LOG.trace("Weaved response with {}: {}", CACHED_KEY, response.get(CACHED_KEY));
    }
    
    private boolean isCacheable(Class<? extends IpcCommand> command) {
        for (Annotation annotation : command.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(ComplexCacheAnnotation.class)) {
                return true;
            }
        }
        return false;
    }
    
}
