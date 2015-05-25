--Minetest
--Copyright (C) 2014 sapier
--
--This program is free software; you can redistribute it and/or modify
--it under the terms of the GNU Lesser General Public License as published by
--the Free Software Foundation; either version 2.1 of the License, or
--(at your option) any later version.
--
--This program is distributed in the hope that it will be useful,
--but WITHOUT ANY WARRANTY; without even the implied warranty of
--MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--GNU Lesser General Public License for more details.
--
--You should have received a copy of the GNU Lesser General Public License along
--with this program; if not, write to the Free Software Foundation, Inc.,
--51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

local function current_game()
    local last_game_id = core.setting_get("menu_last_game")
    local game, index = gamemgr.find_by_gameid(last_game_id)

    return game
end

local function singleplayer_refresh_gamebar()

    local old_bar = ui.find_by_name("game_button_bar")

    if old_bar ~= nil then
        old_bar:delete()
    end

    local function game_buttonbar_button_handler(fields)
        for key,value in pairs(fields) do
            for j=1,#gamemgr.games,1 do
                if ("game_btnbar_" .. gamemgr.games[j].id == key) then
                    mm_texture.update("singleplayer", gamemgr.games[j])
                    --core.set_topleft_text(gamemgr.games[j].name)
                    core.setting_set("menu_last_game",gamemgr.games[j].id)
                    menudata.worldlist:set_filtercriteria(gamemgr.games[j].id)
                    return true
                end
            end
        end
    end

    local btnbar = buttonbar_create("game_button_bar",
        game_buttonbar_button_handler,
        {x=-0.3,y=5.65}, "horizontal", {x=12.4,y=1.15})

    for i=1,#gamemgr.games,1 do
        local btn_name = "game_btnbar_" .. gamemgr.games[i].id

        local image = nil
        local text = nil
        local tooltip = core.formspec_escape(gamemgr.games[i].name)

        if gamemgr.games[i].menuicon_path ~= nil and
            gamemgr.games[i].menuicon_path ~= "" then
            image = core.formspec_escape(gamemgr.games[i].menuicon_path)
        else

            local part1 = gamemgr.games[i].id:sub(1,5)
            local part2 = gamemgr.games[i].id:sub(6,10)
            local part3 = gamemgr.games[i].id:sub(11)

            text = part1 .. "\n" .. part2
            if part3 ~= nil and
                part3 ~= "" then
                text = text .. "\n" .. part3
            end
        end
        btnbar:add_button(btn_name, text, image, tooltip)
    end
end

local function get_formspec(tabview, name, tabdata)

        local index = menudata.worldlist:get_current_index(
                                tonumber(core.setting_get("mainmenu_last_selected_world"))
                                )

        local retval =
            "size[16,11]"..
            "box[-100,8.5;200,10;#999999]" ..
            "box[-100,-10;200,12;#999999]" ..
            "bgcolor[#00000070;true]"..
            "image_button[4,8.7;3.95,0.8;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button.png;play;".. fgettext("Play") .. ";true;true;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button_b.png]"..
            "image_button[7.8,8.7;3.95,0.8;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button.png;world_create;".. fgettext("New") .. ";true;true;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button_b.png]"..

            "image_button[4,9.55;3.95,0.8;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button.png;world_delete;".. fgettext("Delete") .. ";true;true;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button_b.png]"..
            --"image_button[6.53,9.55;2.68,0.8;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button.png;world_configure;".. fgettext("Configure") .. ";true;true;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button_b.png]"..
            "image_button[7.8,9.55;3.95,0.8;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button.png;cancel;".. fgettext("Cancel") .. ";true;true;"..core.formspec_escape(mm_texture.basetexturedir).."menu_button_b.png]"..
            "label[7,1.5;" .. fgettext("Select World:") .. "]" ..

            "checkbox[12,8.70;cb_creative_mode;" .. fgettext("Creative Mode") .. ";" .. dump(core.setting_getbool("creative_mode")) .. "]" ..
            --"checkbox[1000,9.20;cb_enable_damage;" .. fgettext("Enable Damage") .. ";" .. dump(core.setting_getbool("enable_damage")) .. "]" ..
          --  "checkbox[12,9.50;cb_server_announce;" .. fgettext("Public") .. ";" .. dump(core.setting_getbool("server_announce")) .. "]" ..


            "checkbox[0.2,8.35;btn_server;Local Server;false]"


        local bind_addr = core.setting_get("bind_address")

        if not PLATFORM=="android" and bind_addr ~= nil and bind_addr ~= "" then
                retval = retval ..
                        "field[300,0;2.25,0.5;te_serveraddr;" .. fgettext("Bind Address") .. ";" ..
                        core.formspec_escape(core.setting_get("bind_address")) .. "]"..
                        "field[300,1;1.25,0.5;te_serverport;" .. fgettext("Port") .. ";" ..
                        core.formspec_escape(core.setting_get("port")) .. "]"
        else
                retval = retval ..
                        "field[300,1;3.5,0.5;te_serverport;" .. fgettext("Server Port") .. ";" ..
                        core.formspec_escape(core.setting_get("port")) .. "]"
        end

        retval = retval ..
                "textlist[0,2.2;16,6.5;sp_worlds;" ..
                menu_render_worldlist() ..
                ";" .. (index or 1) .. ";true]"

        return retval
end

local function main_button_handler(this, fields, name, tabdata)

    --assert(name == "singleplayer")

    if fields["btn_server"]~=nil then
        local single = create_tab_server(true)
        single:set_parent(this.parent)
        single:show()
        this:hide()
        return true
    end

    local world_doubleclick = false

    if fields["sp_worlds"] ~= nil then
        local event = core.explode_textlist_event(fields["sp_worlds"])

        if event.type == "DCL" then
            world_doubleclick = true
        end

        if event.type == "CHG" then
            core.setting_set("mainmenu_last_selected_world",
                menudata.worldlist:get_raw_index(core.get_textlist_index("sp_worlds")))
            return true
        end
    end

    if menu_handle_key_up_down(fields,"sp_worlds","mainmenu_last_selected_world") then
        return true
    end

    if fields["cb_creative_mode"] then
        core.setting_set("creative_mode", fields["cb_creative_mode"])
        return true
    end

    if fields["cb_enable_damage"] then
        core.setting_set("enable_damage", fields["cb_enable_damage"])
        return true
    end

    if fields["play"] ~= nil or
        world_doubleclick or
        fields["key_enter"] then
        local selected = core.get_textlist_index("sp_worlds")
        print(selected)
        if selected ~= nil then
            gamedata.selected_world = menudata.worldlist:get_raw_index(selected)
            gamedata.singleplayer   = true

            core.start()
        end
        return true
    end

    if fields["world_create"] ~= nil then
        local create_world_dlg = create_create_world_dlg(true)
        create_world_dlg:set_parent(this)
        this:hide()
        create_world_dlg:show()
        mm_texture.update("singleplayer",current_game())
        return true
    end

    if fields["world_delete"] ~= nil then
        local selected = core.get_textlist_index("sp_worlds")
        if selected ~= nil and
            selected <= menudata.worldlist:size() then
            local world = menudata.worldlist:get_list()[selected]
            if world ~= nil and
                world.name ~= nil and
                world.name ~= "" then
                local index = menudata.worldlist:get_raw_index(selected)
                local delete_world_dlg = create_delete_world_dlg(world.name,index)
                delete_world_dlg:set_parent(this)
                this:hide()
                delete_world_dlg:show()
                mm_texture.update("singleplayer",current_game())
            end
        end

        return true
    end

    if fields["world_configure"] ~= nil then
        local selected = core.get_textlist_index("sp_worlds")
        if selected ~= nil then
            local configdialog =
                create_configure_world_dlg(
                        menudata.worldlist:get_raw_index(selected))

            if (configdialog ~= nil) then
                configdialog:set_parent(this)
                this:hide()
                configdialog:show()
                mm_texture.update("singleplayer",current_game())
            end
        end

        return true
    end

    if fields["cancel"] ~= nil then
       this:hide()
       this.parent:show()
       return true
    end

    return false
end

local function on_change(type, old_tab, new_tab)
    local buttonbar = ui.find_by_name("game_button_bar")

    if ( buttonbar == nil ) then
        singleplayer_refresh_gamebar()
        buttonbar = ui.find_by_name("game_button_bar")
    end

    if (type == "ENTER") then
        local game = current_game()

        if game then
            menudata.worldlist:set_filtercriteria(game.id)
            --core.set_topleft_text(game.name)
            mm_texture.update("singleplayer",game)
        end
        buttonbar:show()
    else
        menudata.worldlist:set_filtercriteria(nil)
        buttonbar:hide()
        --core.set_topleft_text("")
        mm_texture.update(new_tab,nil)
    end
end

--------------------------------------------------------------------------------
tab_singleplayer = {
    name = "singleplayer",
    caption = fgettext("Singleplayer"),
    cbf_formspec = get_formspec,
    cbf_button_handler = main_button_handler,
    on_change = on_change
    }

function create_tab_single()
        local retval = dialog_create("singleplayer",
                                        get_formspec,
                                        main_button_handler,
                                        nil)
        return retval
end

