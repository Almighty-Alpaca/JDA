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

import net.dv8tion.jda.entities.impl.GuildImpl;
import net.dv8tion.jda.entities.impl.JDAImpl;
import net.dv8tion.jda.entities.impl.UserImpl;
import net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent;
import org.json.JSONObject;

public class GuildMemberRemoveHandler extends SocketHandler
{

    public GuildMemberRemoveHandler(JDAImpl api, int responseNumber)
    {
        super(api, responseNumber);
    }

    @Override
    public void handle(JSONObject content)
    {
        GuildImpl guild = (GuildImpl) api.getGuildMap().get(content.getString("guild_id"));
        UserImpl user = ((UserImpl) api.getUserMap().get(content.getJSONObject("user").getString("id")));
        guild.getUserRoles().remove(user);
        if (!api.getGuildMap().values().stream().anyMatch(g -> ((GuildImpl) g).getUserRoles().containsKey(user)))
        {
            if (user.hasPrivateChannel())
            {
                api.getOffline_pms().put(user.getId(), user.getPrivateChannel().getId());
            }
            api.getUserMap().remove(user.getId());
        }
        api.getEventManager().handle(
                new GuildMemberLeaveEvent(
                        api, responseNumber,
                        guild, user));
    }
}
