/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util.poi;

import com.dotmarketing.util.Logger;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * <a href="XLSTextStripper.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Mirco Tamburini
 * @version $Revision: 1.4 $
 *
 */
public class XLSTextStripper {

	public XLSTextStripper(final InputStream fis) {
		try {
			StringBuffer sb = new StringBuffer();

			HSSFWorkbook workbook = new HSSFWorkbook(fis);

			int numOfSheets = workbook.getNumberOfSheets();

			for(int i = 0; i < numOfSheets; i++) {
				HSSFSheet sheet = workbook.getSheetAt(i);

				Iterator rowIterator = sheet.rowIterator();

				while (rowIterator.hasNext()) {
					HSSFRow row = (HSSFRow)rowIterator.next();

					Iterator cellIterator = row.cellIterator();

					while (cellIterator.hasNext()) {
						HSSFCell cell = (HSSFCell)cellIterator.next();

						String cellStringValue = null;

						if (cell.getCellType() == 4) {
							boolean booleanValue = cell.getBooleanCellValue();
							cellStringValue = Boolean.toString(booleanValue);
						}
						else if (cell.getCellType() == 0) {
							double doubleValue = cell.getNumericCellValue();
							cellStringValue = Double.toString(doubleValue);
						}
						else if (cell.getCellType() == 1) {
							cellStringValue = cell.getStringCellValue();
						}

						if (cellStringValue != null) {
							sb.append(cellStringValue);
							sb.append("\t");
						}
					}

					sb.append("\n");
				}
			}

			_text = sb.toString();
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	public String getText() {
		return  _text;
	}

	private String _text;

}