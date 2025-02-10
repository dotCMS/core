/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.validation;



public class ColorValue {

	// all private
	private int red;
	private int green;
	private int blue;
	
	private boolean valid;

	public ColorValue(String color) {
		
		color = color.replaceAll(" ", "");	// remove whitespaces

		if ( color.startsWith("#")) color = color.substring(1);
	  
		this.valid = true;	// set default
				
		if ( color.length() == 6 ) {
		
			red = Integer.valueOf(color.substring(0, 2), 16);
			green = Integer.valueOf(color.substring(2, 4), 16);
			blue = Integer.valueOf(color.substring(4, 6), 16);
		    
		}
		else if ( color.length() == 3 ) {
			
			red = Integer.valueOf(color.substring(0, 1) + color.substring(0, 1), 16);
			green = Integer.valueOf(color.substring(1, 2) + color.substring(1, 2), 16);
			blue = Integer.valueOf(color.substring(2, 3) + color.substring(2, 3), 16);
			
		}
		else if ( color.startsWith("rgb(")) {
			String colorLine = color.substring(4, color.length() - 1);
			String[] colors = colorLine.split(",");

			red = Integer.parseInt(colors[0]);
			green = Integer.valueOf(colors[1]);
			blue = Integer.valueOf(colors[2]);

		}
//		else { // color name
//			
//			$colorMappingDAO = new ColorMappingDAO();
//			$rows = $colorMappingDAO->GetByColorName($color);
//
////			$sql = "SELECT color_code FROM ". TABLE_PREFIX ."color_mapping WHERE color_name='".$color."'";
////			$result	= mysql_query($sql, $db) or die(mysql_error());
//			
//			if (!is_array($rows) == 0)
//				$this->isValid = false;
//			else
//			{
//				$row = $rows[0];
//				$colorCode = $row["color_code"];
//
//		    list($r, $g, $b) = array($colorCode[0].$colorCode[1],
//		                             $colorCode[2].$colorCode[3],
//		                             $colorCode[4].$colorCode[5]);
//			}
//
//		}

	}

	/**
	* public
	* Return if the color value is valid
	*/
	public boolean isValid() {
		return this.valid;
	}

	/**
	* public
	* Return red value
	*/
	public int getRed()
	{
		return this.red;
	}

	/**
	* public
	* Return green value
	*/
	public int getGreen()
	{
		return this.green;
	}

	/**
	* public
	* Return blue value
	*/
	public int getBlue()
	{
		return this.blue;
	}
}
