/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
