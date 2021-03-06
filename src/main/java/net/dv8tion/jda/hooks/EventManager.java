/**
 *    Copyright 2015 Austin Keener & Michael Ritter
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
package net.dv8tion.jda.hooks;

import net.dv8tion.jda.events.Event;

import java.util.LinkedList;
import java.util.List;

public class EventManager
{
    private final List<EventListener> listeners = new LinkedList<>();

    public EventManager()
    {

    }

    public void register(EventListener listener)
    {
        listeners.add(listener);
    }

    public void unregister(EventListener listener)
    {
        listeners.remove(listener);
    }

    public void handle(Event event)
    {
        for (EventListener listener : listeners)
        {
            listener.onEvent(event);
        }
    }
}
