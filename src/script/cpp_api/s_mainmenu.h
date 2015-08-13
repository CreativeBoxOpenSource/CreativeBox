/*
Minetest
Copyright (C) 2013 celeron55, Perttu Ahola <celeron55@gmail.com>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation; either version 3.0 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

#ifndef S_MAINMENU_H_
#define S_MAINMENU_H_

#include "cpp_api/s_base.h"
#include "util/string.h"

class ScriptApiMainMenu
		: virtual public ScriptApiBase
{
public:
	/**
	 * set gamedata.errormessage to inform lua of an error
	 * @param errormessage the error message
	 */
	void setMainMenuErrorMessage(std::string errormessage);

	/**
	 * process events received from formspec
	 * @param text events in textual form
	 */
	void handleMainMenuEvent(std::string text);

	/**
	 * process field data recieved from formspec
	 * @param fields data in field format
	 */
	void handleMainMenuButtons(const StringMap &fields);
};

#endif /* S_MAINMENU_H_ */
