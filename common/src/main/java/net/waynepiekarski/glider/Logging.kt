// ---------------------------------------------------------------------
//
// Glider
//
// Copyright (C) 1996-2014 Wayne Piekarski
// wayne@tinmith.net http://tinmith.net/wayne
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// ---------------------------------------------------------------------

package net.waynepiekarski.glider

import android.util.Log

object Logging {

    private val TAG = "Glider"

    fun debug(str: String) {
        Log.d(TAG, str)
    }

    fun fatal(str: String) {
        Log.e(TAG, "FATAL ERROR: " + str)
        val re = RuntimeException()
        re.printStackTrace()
        Log.e(TAG, "Exiting with error code 1")
        System.exit(1)
    }
}
