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

function validateImageFileName(fileName) {
	if (fileName == null || fileName == "") {
		return false;
	}

	if (fileName.toLowerCase().indexOf(".gif") == fileName.length - 4 ||
		fileName.toLowerCase().indexOf(".jpg") == fileName.length - 4 ||
		fileName.toLowerCase().indexOf(".png") == fileName.length - 4) {

		return true;
	}

	return false;
}

function removeDots(val)
{
  
	var result = '';
	vals = val.split('.');
	for(i=0;i<vals.length;i++)
	{
		result+= vals[i];
	}
	
	return result;
}

//Verifica que se un valor flotante con las precision especificada en A,B al estilo BD Number(A,B)
function isFloat(campo,A,B){		
	if (campo != "")
	{
		A = A - B;
		if (!(isInteger(campo,A))){
			if (validateDots(campo.split(',')[0]))
			{					
				campo = removeDots(campo);				
				var regular = eval ("/^\\d{1," + A + "}[,]\\d{1," + B + "}$/");
				var resultado = regular.exec(campo);
				if (resultado == null)
					return false;
				else
					return true;
			}
			else return false;
		}			
		return true;
	}
	else return true;
}

//Verifica que se un valor Integer con la presicion especificada por A al estilo BD Number(A)
function isInteger(campo,A){
	if(validateDots(campo))
	{		
		campo = removeDots(campo);
		var regular = eval ("/^-?\\d{0," + A + "}$/");
		//alert (regular)
		var resultado = regular.exec(campo);
		//alert(resultado+(resultado==null));
		if (resultado == null)
			return false;
		else
			return true;
	}
	else return false;
}

function validateDots(val)
{	
	result = true;
	vals = val.split('.');
	if (vals.length == 1)
	{
		result = true;
	}
	else
	{
		for(i=1; i<vals.length; i++)
		{
			if (vals[i].length != 3)
			{
				result = false;
				break;
			}
		}	
		if (vals[0].length > 3)
			result = false;	
	}
	return result;
}