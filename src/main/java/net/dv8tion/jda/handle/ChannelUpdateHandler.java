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
package net.dv8tion.jda.handle;

import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.impl.*;
import net.dv8tion.jda.events.channel.text.TextChannelUpdateNameEvent;
import net.dv8tion.jda.events.channel.text.TextChannelUpdatePermissionsEvent;
import net.dv8tion.jda.events.channel.text.TextChannelUpdatePositionEvent;
import net.dv8tion.jda.events.channel.text.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.events.channel.voice.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.events.channel.voice.VoiceChannelUpdatePermissionsEvent;
import net.dv8tion.jda.events.channel.voice.VoiceChannelUpdatePositionEvent;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelUpdateHandler extends SocketHandler
{

    public ChannelUpdateHandler(JDAImpl api, int responseNumber)
    {
        super(api, responseNumber);
    }

    @Override
    public void handle(JSONObject content)
    {
        List<Role> changedRoles = new ArrayList<>();
        List<User> changedUsers = new ArrayList<>();
        List<Role> containedRoles = new ArrayList<>();
        List<User> containedUsers = new ArrayList<>();

        String name = content.getString("name");
        int position = content.getInt("position");
        JSONArray permOverwrites = content.getJSONArray("permission_overwrites");
        switch (content.getString("type"))
        {
            case "text":
            {
                String topic = content.isNull("topic") ? null : content.getString("topic");
                TextChannelImpl channel = (TextChannelImpl) api.getChannelMap().get(content.getString("id"));
                if (channel == null)
                    throw new IllegalArgumentException("CHANNEL_UPDATE attemped to update a TextChannel that does not exist. JSON: " + content);

                //If any properties changed, update the values and fire the proper events.
                if (!StringUtils.equals(channel.getName(), name))
                {
                    String oldName = channel.getName();
                    channel.setName(name);
                    api.getEventManager().handle(
                            new TextChannelUpdateNameEvent(
                                    api, responseNumber,
                                    channel, oldName));
                }
                if (!StringUtils.equals(channel.getTopic(), topic))
                {
                    String oldTopic = channel.getTopic();
                    channel.setTopic(topic);
                    api.getEventManager().handle(
                            new TextChannelUpdateTopicEvent(
                                    api, responseNumber,
                                    channel, oldTopic));
                }
                if (channel.getPosition() != position)
                {
                    int oldPosition = channel.getPosition();
                    channel.setPosition(position);
                    api.getEventManager().handle(
                            new TextChannelUpdatePositionEvent(
                                    api, responseNumber,
                                    channel, oldPosition));
                }

                //Determines if a new PermissionOverride was created or updated.
                //If a PermissionOverride was created or updated it stores it in the proper Map to be reported by the Event.
                for (int i = 0; i < permOverwrites.length(); i++)
                {
                    JSONObject override = permOverwrites.getJSONObject(i);
                    PermissionOverride newOverride = new PermissionOverride(override.getInt("allow"), override.getInt("deny"));
                    switch (override.getString("type"))
                    {
                        case "role":
                        {
                            Role role = ((GuildImpl) channel.getGuild()).getRolesMap().get(override.getString("id"));
                            if (role == null)
                                throw new IllegalArgumentException("CHANNEL_UPDATE attempted to create or update a PermissionOverride for a Role that doesn't exist! JSON: " + content);
                            PermissionOverride oldOverride = channel.getRolePermissionOverrides().get(role);

                            if (oldOverride == null || !newOverride.equals(oldOverride))    //updated
                            {
                                changedRoles.add(role);
                                channel.getRolePermissionOverrides().put(role, newOverride);
                            }
                            containedRoles.add(role);
                            break;
                        }
                        case "member":
                        {
                            User user = api.getUserMap().get(override.getString("id"));
                            if (user == null)
                                throw new IllegalArgumentException("CHANNEL_UPDATE attempted to create or update a PermissionOverride for User that doesn't exist! JSON: " + content);
                            PermissionOverride oldOverride = channel.getUserPermissionOverrides().get(user);

                            if (oldOverride == null || !oldOverride.equals(newOverride))    //Created
                            {
                                changedUsers.add(user);
                                channel.getUserPermissionOverrides().put(user, newOverride);
                            }
                            containedUsers.add(user);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("CHANNEL_UPDATE provided an unrecognized PermissionOverride type. JSON: " + content);
                    }
                }

                //Check if any overrides were deleted because of this event.
                //Get the current overrides. (we copy them to a new list because the Set returned is backed by the Map, meaning our removes would remove from the Map. Not good.
                //Loop through all of the json defined overrides. If we find a match, remove the User or Role from our lists.
                //Any entries remaining in these lists after this for loop is over will be removed from the Channel's overrides.
                List<Role> collect = channel.getRolePermissionOverrides().keySet().stream().filter(role -> !containedRoles.contains(role)).collect(Collectors.toList());
                collect.forEach(role -> {
                    changedRoles.add(role);
                    channel.getRolePermissionOverrides().remove(role);
                });
                List<User> collect1 = channel.getUserPermissionOverrides().keySet().stream().filter(user -> !containedUsers.contains(user)).collect(Collectors.toList());
                collect1.forEach(user -> {
                    changedUsers.add(user);
                    channel.getUserPermissionOverrides().remove(user);
                });

                //If this update modified permissions in any way.
                if (!changedRoles.isEmpty()
                        || !changedUsers.isEmpty())
                {
                    api.getEventManager().handle(
                            new TextChannelUpdatePermissionsEvent(
                                    api, responseNumber,
                                    channel,
                                    changedRoles, changedUsers));
                }
                break;  //Finish the TextChannelUpdate case
            }
            case "voice":
            {
                VoiceChannelImpl channel = (VoiceChannelImpl) api.getVoiceChannelMap().get(content.getString("id"));
                if (channel == null)
                    throw new IllegalArgumentException("CHANNEL_UPDATE attemped to update a VoiceChannel that does not exist. JSON: " + content);

                //If any properties changed, update the values and fire the proper events.
                if (!StringUtils.equals(channel.getName(), name))
                {
                    String oldName = channel.getName();
                    channel.setName(name);
                    api.getEventManager().handle(
                            new VoiceChannelUpdateNameEvent(
                                    api, responseNumber,
                                    channel, oldName));
                }
                if (channel.getPosition() != position)
                {
                    int oldPosition = channel.getPosition();
                    channel.setPosition(position);
                    api.getEventManager().handle(
                            new VoiceChannelUpdatePositionEvent(
                                    api, responseNumber,
                                    channel, oldPosition));
                }

                //Determines if a new PermissionOverride was created or updated.
                //If a PermissionOverride was created or updated it stores it in the proper Map to be reported by the Event.
                for (int i = 0; i < permOverwrites.length(); i++)
                {
                    JSONObject override = permOverwrites.getJSONObject(i);
                    PermissionOverride newOverride = new PermissionOverride(override.getInt("allow"), override.getInt("deny"));
                    switch (override.getString("type"))
                    {
                        case "role":
                        {
                            Role role = ((GuildImpl) channel.getGuild()).getRolesMap().get(override.getString("id"));
                            if (role == null)
                                throw new IllegalArgumentException("CHANNEL_UPDATE attempted to create or update a PermissionOverride for a Role that doesn't exist! JSON: " + content);
                            PermissionOverride oldOverride = channel.getRolePermissionOverrides().get(role);

                            if (oldOverride == null || !newOverride.equals(oldOverride))    //updated
                            {
                                changedRoles.add(role);
                                channel.getRolePermissionOverrides().put(role, newOverride);
                            }
                            containedRoles.add(role);
                            break;
                        }
                        case "member":
                        {
                            User user = api.getUserMap().get(override.getString("id"));
                            if (user == null)
                                throw new IllegalArgumentException("CHANNEL_UPDATE attempted to create or update a PermissionOverride for User that doesn't exist! JSON: " + content);
                            PermissionOverride oldOverride = channel.getUserPermissionOverrides().get(user);

                            if (oldOverride == null || !oldOverride.equals(newOverride))    //Created
                            {
                                changedUsers.add(user);
                                channel.getUserPermissionOverrides().put(user, newOverride);
                            }
                            containedUsers.add(user);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("CHANNEL_UPDATE provided an unrecognized PermissionOverride type. JSON: " + content);
                    }
                }

                //Check if any overrides were deleted because of this event.
                //Get the current overrides. (we copy them to a new list because the Set returned is backed by the Map, meaning our removes would remove from the Map. Not good.
                //Loop through all of the json defined overrides. If we find a match, remove the User or Role from our lists.
                //Any entries remaining in these lists after this for loop is over will be removed from the Channel's overrides.
                channel.getRolePermissionOverrides().keySet().stream().filter(role -> !containedRoles.contains(role)).forEach(role -> {
                    changedRoles.add(role);
                    channel.getRolePermissionOverrides().remove(role);
                });
                channel.getUserPermissionOverrides().keySet().stream().filter(user -> !containedUsers.contains(user)).forEach(user -> {
                    changedUsers.add(user);
                    channel.getUserPermissionOverrides().remove(user);
                });

                //If this update modified permissions in any way.
                if (!changedRoles.isEmpty()
                        || !changedUsers.isEmpty())
                {
                    api.getEventManager().handle(
                            new VoiceChannelUpdatePermissionsEvent(
                                    api, responseNumber,
                                    channel,
                                    changedRoles, changedUsers));
                }
                break;  //Finish the TextChannelUpdate case
            }
            default:
                throw new IllegalArgumentException("CHANNEL_UPDATE provided an unrecognized channel type JSON: " + content);
        }
    }
}
