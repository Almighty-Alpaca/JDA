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
package net.dv8tion.jda.events.guild.member;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;

import java.util.Collections;
import java.util.List;

public class GuildMemberRoleAddEvent extends GenericGuildMemberEvent
{
    private final List<Role> addedRoles;

    public GuildMemberRoleAddEvent(JDA api, int responseNumber, Guild guild, User user, List<Role> addedRoles)
    {
        super(api, responseNumber, guild, user);
        this.addedRoles = addedRoles;
    }

    public List<Role> getRoles()
    {
        return Collections.unmodifiableList(addedRoles);
    }
}
