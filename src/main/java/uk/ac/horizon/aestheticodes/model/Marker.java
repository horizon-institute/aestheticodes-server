/*
 * Aestheticodes recognises a different marker scheme that allows the
 * creation of aesthetically pleasing, even beautiful, codes.
 * Copyright (C) 2013-2015  The University of Nottingham
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.horizon.aestheticodes.model;

import java.util.Comparator;

public class Marker
{
	public static final Comparator<Marker> comparator = new Comparator<Marker>()
	{
		@Override
		public int compare(Marker marker1, Marker marker2)
		{
			if (marker1.getCode().length() != marker2.getCode().length())
			{
				return marker1.getCode().length() - marker2.getCode().length();
			}
			return marker1.getCode().compareTo(marker2.getCode());
		}
	};

	private String code;
	private String action;
	private String title;
	private String description;
	private String image;
	private boolean showDetail = false;

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public String getImage()
	{
		return image;
	}

	public void setImage(String image)
	{
		this.image = image;
	}

	public boolean getShowDetail()
	{
		return showDetail;
	}

	public void setShowDetail(boolean showDetail)
	{
		this.showDetail = showDetail;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
