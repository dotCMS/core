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


public class HTMLValidator {

//	// all private
//	var $validator_url = "http://validator.w3.org/check";   // url of 3rd party validator
//	
//	var $full_return; 						// full return from the 3rd party validator
//	var $result;                  // result section stripped from $full_return
//	var $result_array;            // result values stripped from $full_return in form of array
//	var $num_of_errors = 0;       // number of errors
//	
//	var $contain_errors = false;  // true or false. if error happens in process
//	var $msg;                     // not null when $contain_errors is true. detail error message in process
//	
//	var $validate_type;           // uri or fragment
//	var $validate_content;        // uri or html content
//	
//	/**
//	* private
//    * main process
//	*/
//	function HTMLValidator($type, $content, $return_array = false)
//	{
//		$this->validate_type = $type;
//		$this->validate_content = $content;
//		
//		if ($this->validate_type == "fragment")
//			$result = $this->validate_fragment($this->validate_content);			
//		if ($this->validate_type == "uri")
//		{
//			if (Utility::getValidURI($this->validate_content) === false)
//			{
//				$this->contain_errors = true;
//				$this->msg = "Error: Cannot connect to <strong>".$uri. "</strong>";
//				return false;
//			}
//			$result = $this->validate_uri($this->validate_content);
//		}
//
//		if (!result) return false;
//		else
//		{						
//			$this->full_return = $result;
//			if ($return_array == true) {				
//				$this->stripOutResultArray();
//			} else {
//				$this->result = $this->stripOutResult($result);
//			}
//			$this->num_of_errors = $this->stripOutNumOfErrors($result);
//						
//			return true;
//		}
//	}
//	
//	/**
//	* private
//    * send uri to 3rd party and return its response
//	*/
//	function validate_uri($uri)
//	{
//		return file_get_contents($this->validator_url. "?uri=".$uri);
//	}
//	
//	/**
//	* private
//    * send fragment to 3rd party and return its response
//	*/
//	function validate_fragment($fragment)
//	{
//		$data = array ('fragment' => $fragment, 'output' => 'html');
//		
//		$data = http_build_query($data);
//		
//		$response = $this->do_post_request($this->validator_url, $data);
//		
//		return $response;
//	}
//
//	/**
//	* private
//  	* send post request and html content to 
//	*/
//  function do_post_request($url, $data, $optional_headers = null)
//  {
//     $params = array('http' => array(
//                  'method' => 'POST',
//                  'content' => $data
//               ));
//     if ($optional_headers !== null) {
//        $params['http']['header'] = $optional_headers;
//     }
//     $ctx = stream_context_create($params);
//
//     if (!($fp = @fopen($url, 'rb', false, $ctx))) 
//     {
//				$this->contain_errors = true;
//				$this->msg = "Problem with $url, $php_errormsg";
//				return false;
//     }
//     $response = @stream_get_contents($fp);
//     if ($response === false) 
//     {
//				$this->contain_errors = true;
//				$this->msg = "Problem reading data from $url, $php_errormsg";
//				return false;
//     }
//     return $response;
//  }
//  
//	/**
//	* private
//	* return errors/warnings by striping it out from validation output returned from 3rd party
//	*/
//	function stripOutResult($full_result)
//	{
//		$pattern1 = '/'. preg_quote('<div id="result">', '/') . '.*'. preg_quote('</div><!-- end of "result" -->', '/').'/s';   // when no errors
//		$pattern2 = '/('.preg_quote('<ol id="error_loop">', '/').'.*'. preg_quote('</ol>', '/').')/s'; // when has errors
//
//		if (preg_match($pattern1, $full_result, $matches))
//			return $matches[0];
//		else if (preg_match($pattern2, $full_result, $matches))
//		{
//			$result = $matches[1];
//			
//			$search = array('src="images/info_icons/error.png"',
//											'src="images/info_icons/info.png"',
//											'src="images/info_icons/warning.png"');
//			$replace = array('src="images/error.png" width="15" height="15"',
//											'src="images/info.png" width="15" height="15"',
//											'src="images/warning.png" width="15" height="15"');
//			
//			return str_replace($search, $replace, $result);
//		}
//		else
//		{
//			$this->contain_errors = true;
//			$this->msg = "Cannot find result report from the return of the validator";
//			return false;
//		}
//	}
//	
//	/**
//	* private
//	* return errors/warnings in array form by striping it out from validation output returned from 3rd party
//	*/
//	function stripOutResultArray()
//	{	
//		$pattern1 = '/'. preg_quote('<div id="result">', '/') . '.*'. preg_quote('</div><!-- end of "result" -->', '/').'/s';   // when no errors
//		$pattern2 = '/('.preg_quote('<ol id="error_loop">', '/').'.*'. preg_quote('</ol>', '/').')/s'; // when has errors
//		if (preg_match($pattern1, $this->full_return, $match) || preg_match($pattern2, $this->full_return, $match))	{	
//			$pattern_item = '/(('.preg_quote('<li class="msg_err">', '/').'|'.preg_quote('<li class="msg_warn">', '/').'|'.preg_quote('<li class="msg_info">', '/').')(.*?)'. preg_quote('</li>', '/').'(?>(\r?\n){2}))/s';
//			preg_match_all($pattern_item, $this->full_return, $matches);
//			foreach($matches[3] as $error) {		
//				// img_src
//				$pattern_img_src = '/('.preg_quote('<img src="images/info_icons/', '/').'(.*)" alt)/';
//				if (preg_match($pattern_img_src, $error, $matches_img_src)) {
//					$img_src =  $matches_img_src[2];
//				} else {
//					$img_src = '';
//				}
//			
//				// line and column
//				$pattern_line_col = '/('.preg_quote('<em>', '/').'(.*?)'. preg_quote('</em>', '/').')/s';	
//				if (preg_match($pattern_line_col, $error, $matches_line_col)) {
//					$pattern_line = '/(Line (.*?),)/';			
//					preg_match($pattern_line, $matches_line_col[2], $matches_line);
//					$line = $matches_line[2];
//					
//					$pattern_col = '/(Column (.*?)$)/';
//					preg_match($pattern_col, $matches_line_col[2], $matches_col);
//					$col = $matches_col[2];
//					
//				} else {
//					$col = '';
//					$line = '';
//				}
//				
//				// error
//				$pattern_error = '/('.preg_quote('<span class="msg">', '/').'(.*)'. preg_quote('</span>', '/').')/';	
//				if (preg_match($pattern_error, $error, $matches_error)) {
//					$err =  $matches_error[2];
//				} else {
//					$err = '';
//				}
//				
//				// html 
//				$pattern_html_1 = '/('.preg_quote('<code class="input">', '/').'(.*)'. preg_quote('<strong', '/').')/';	
//				if (preg_match($pattern_html_1, $error, $matches_html_1)) {
//					$html_1 = $matches_html_1[2];
//				} else {
//					$html_1 = '';
//				}
//				
//				$pattern_html_2 = '/('.preg_quote('<strong title="Position where error was detected.">', '/').'(.*)'. preg_quote('</strong>', '/').')/';	
//				if (preg_match($pattern_html_2, $error, $matches_html_2)) {
//					$html_2 = $matches_html_2[2];
//				} else {
//					$html_2 = '';
//				}
//				
//				$pattern_html_3 = '/('.preg_quote('</strong>', '/').'(.*)'. preg_quote('</code>', '/').')/';	
//				if (preg_match($pattern_html_3, $error, $matches_html_3)) {
//					$html_3 = $matches_html_3[2];
//				} else {
//					$html_3 = '';
//				}
//				
//				// text
//				$pattern_text = '/(\<div class="[A-Za-z0-9- ]+">(.*)'. preg_quote('</div>', '/').')/s';	
//				if (preg_match($pattern_text, $error, $matches_text)) {
//					$text = trim($matches_text[2]);
//				} else {
//					$text = '';
//				}
//				$this->result_array[] = array('img_src' => $img_src, 'col' => $col, 'line' => $line, 'err' => $err, 
//					'html_1' => $html_1, 'html_2' => $html_2, 'html_3' => $html_3, 'text' => $text); 
//			} 
//		} else {
//			$this->contain_errors = true;
//			$this->msg = "Cannot find result report from the return of the validator";
//			return false;
//		}
//		
//	}
//	
//	/**
//	* private
//	* return number of errors by striping it out from validation output returned from 3rd party
//	*/
//	function stripOutNumOfErrors($full_result)
//	{
//		$pattern1 = '/' .preg_quote('<th>Result:</th>', '/').'\s*'.preg_quote('<td colspan="2" class="invalid">', '/').
//								'\s*(\w+) Error/s';   // when has errors
//
//		// when has errors
//		if (preg_match($pattern1, $full_result, $matches))  // when has errors
//			return $matches[1];
//		else
//		{
//			return 0;
//		}
//	}
//	
//	/**
//	* public 
//	* return validation report in html
//	*/
//	function getValidationRpt()
//	{
//		return $this->result;
//	}
//	
//	/**
//	* public 
//	* return validation report in form of array
//	*/
//	function getValidationRptArray()
//	{
//		return $this->result_array;
//	}
//
//	// public 
//	function getNumOfValidateError()
//	{
//		return $this->num_of_errors;
//	}
//
//	/**
//	* public 
//	* return error message
//	*/
//	function getErrorMsg()
//	{
//		return $this->msg;
//	}
//	
//	/**
//	* public 
//	* return true or false: if error happens during process
//	*/
//	function containErrors()
//	{
//		return $this->contain_errors;
//	}
	
}

