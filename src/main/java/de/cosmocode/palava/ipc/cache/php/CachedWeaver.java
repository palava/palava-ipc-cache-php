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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.cosmocode.commons.reflect.Reflection;
import de.cosmocode.palava.core.Registry;
import de.cosmocode.palava.ipc.cache.Cached;
import de.cosmocode.palava.ipc.json.custom.CustomPostCallEvent;
import de.cosmocode.palava.ipc.json.custom.CustomProtocol;
import de.cosmocode.palava.ipc.protocol.DetachedConnection;

/**
 * Adds a flag to the response, that the command may be cached.
 *
 * @author Tobias Sarnowski
 */
final class CachedWeaver implements CustomPostCallEvent {
    
    private static final Logger LOG = LoggerFactory.getLogger(CachedWeaver.class);

    // for php
    private static final String CACHED_KEY = "CACHED";

    @Inject
    CachedWeaver(Registry registry) {
        registry.register(CustomPostCallEvent.class, this);
    }

    @Override
    public void eventPostCall(Map<String, Object> request, Map<String, Object> response, 
        DetachedConnection connection) {
        // get the command class manually
        final String cmd = String.class.cast(request.get(CustomProtocol.COMMAND));
        final Class<?> command;
        
        try {
            command = Reflection.forName(cmd);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot retrieve the command " + cmd + " as class", e);
        }

        // weave the "CACHED" constant into the meta informations
        response.put(CACHED_KEY, command.isAnnotationPresent(Cached.class));
        LOG.trace("Weaved response with {}: {}", CACHED_KEY, response.get(CACHED_KEY));
    }
    
}
