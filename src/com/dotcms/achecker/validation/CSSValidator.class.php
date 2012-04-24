<?php
/************************************************************************/
/* AChecker                                                             */
/************************************************************************/
/* Copyright (c) 2008 - 2011                                            */
/* Inclusive Design Institute                                           */
/*                                                                      */
/* This program is free software. You can redistribute it and/or        */
/* modify it under the terms of the GNU General Public License          */
/* as published by the Free Software Foundation.                        */
/************************************************************************/
// $Id$

/**
* CSSValidator
* Class for css validate
* This class sends css url to 3rd party validator
* and retrieve the returned results.
* @access	public
* @author	Simone Spagnoli
* @package checker
*/

include_once(AC_INCLUDE_PATH.'classes/Utility.class.php');

class CSSValidator {

	var $validator_url = "http://jigsaw.w3.org/css-validator/validator";
	
	// The variables to the locally installed css validator
	// NOT IN USE generally. Handbook explanation is required to make use of this feature.
	var $local_validator_command = "java -jar ";
	var $local_validator_path = "../local_validator/local_css_validator/css-validator.jar "; // path validatore 
	var $local_validator_option = "--output=xhtml --warning=0 --lang=it ";
	
	var $full_return;             // full return from the 3rd party validator
	var $result;                  // result section stripped from $full_returns.spagnoli@unibo.it
	var $result_array;            // result section stripped from $this->result in form of array
	var $num_of_errors = 0;       // number of errors
	
	var $contain_errors = false;  // true or false. if error happens in process
	var $msg;                     // not null when $contain_errors is true. detail error message in process
	
	var $validate_type;           // uri or fragment
	var $validate_content;        // uri or html content
	
	/**
	* private
  * main process
	*/
	function CSSValidator($type, $content, $return_array = false)
	{
		$this->validate_type = $type;
		$this->validate_content = $content;

		if ($this->validate_type == "uri")
		{
			if (Utility::getValidURI($this->validate_content) === false)
			{
				$this->contain_errors = true;
				$this->msg = _AC('AC_ERROR_CANT_CONNECT', $uri); //"Error: Cannot connect to <strong>".$uri. "</strong>";
				return false;
			}
			$result = $this->validate_uri($this->validate_content);
		} else {
			return false;  // css validator is only available for url checking, not for file upload and paste 
		}

		if (!$result) return false;
		else
		{
			$this->full_return = $result;
			$this->result = $this->stripOutResult($result);
			if ($return_array == true) {
				$this->stripOutResultArray();
			}
			$this->num_of_errors = $this->stripOutNumOfErrors($result);
			return true;
		}
	}
	
	/**
	* private
  * send uri to 3rd party and return its response
	*/
	function validate_uri($uri)
	{

		$sys_command = $this->local_validator_command . $this->local_validator_path . $this->local_validator_option;

		exec($sys_command . $uri, $retval);

		if (sizeof($retval) == 0)
		{
			// If output from the internal validiator is not found, use the external validator
			include_once(AC_INCLUDE_PATH.'classes/DAO/LangCodesDAO.class.php');
			
			$langCodesDAO = new LangCodesDAO();
			$lang_code_2letters = $langCodesDAO->GetLangCodeBy3LetterCode($_SESSION['lang']);
			// needed, a way to switch profiles, here defaulting to css3 (1, 2, 2.1, 3)
			$content = @file_get_contents($this->validator_url. "?uri=".$uri."&warning=0&profile=css3&lang=".$lang_code_2letters["code_2letters"]);
		}
		else {
			//echo "validatore interno";
			$content = implode($retval);
		}
	
		if ($content != null)
		{
			return $content;
		}
		return false;	
	}

  
	/**
	* private
	* return errors/warnings by striping it out from validation output returned from 3rd party
	*/
	function stripOutResult($full_result)
	{
		$pattern1 = '/('.preg_quote("<div id='congrats'>", '/').'.*)/s'; // nessun errore -- no errors
		$pattern2 = '/('.preg_quote("<div class='error-section-all'>", '/').'.*)/s'; // when has errors
		$pattern3 = '/('.preg_quote('<p class="backtop"><a href="#banner">&uarr; ', '/').'.*)/s'; // when has errors
		$pattern4 = '/('.preg_quote('<div id="warnings">', '/').'.*)/s'; // when has errors
	
		if (preg_match($pattern1, $full_result, $matches)){
			return $matches[0];
		}
		else if (preg_match($pattern2, $full_result, $matches))
		{
			if (preg_match($pattern3, $full_result, $matches2))
			{
				$result_exp = explode('<p class="backtop"><a href="#banner">', $matches[0]);
			}
			else if (preg_match($pattern4, $full_result, $matches2))
			{
				$result_exp = explode('<div id="warnings">', $matches[0]);
			}
			$result = $result_exp[0];
			
			$res_exp = explode("<div class='error-section'>", $result);
			
			// Formatta il risultato - format the results
			for ($i=0; $i<sizeof($res_exp); $i++)
			{
				if ($i==0)
				{		
					// "primo ciclo";
					$res_exp[$i] = str_replace("<div class='error-section-all'>", "<ul class='msg_err'>", $res_exp[$i]);
					//$res_exp[$i] = str_replace('<div id="errors">', "<ol>", $res_exp[$i]);
					
				}
				elseif ( $i==(sizeof($res_exp)-1) )
				{
					// "ultimo ciclo";
					$res_exp[$i] =  '<li class="msg_err">'. $res_exp[$i];
					$res_exp[$i] = str_replace("<h4>", '<span class="msg"><strong>', $res_exp[$i]);
					$res_exp[$i] = str_replace("</h4>", '</strong></span>', $res_exp[$i]);
					$res_exp[$i] = str_replace("<table>", "<table class='css_error' cellspacing='4px'><tr><th>"._AC('line')."</th><th>"._AC('html_tag')."</th><th>"._AC('error')."</th></tr>", $res_exp[$i]);
					
					$res_exp_int = explode("</div>", $res_exp[$i]);	
					for ($j=0; $j<sizeof($res_exp_int)-1; $j++)
					{
						if ($j==0)
						{		
							//"primo ciclo interno";
							$res_exp_int[$j] = $res_exp_int[$j] . "</li>";
						}
						else if ($j==sizeof($res_exp_int)-2)
						{		
							//"ultimo ciclo interno - last inner loop";
						//	$res_exp_int[$j] = "</div>";
						}
						else 
						{
							//"ciclo interno";
							$res_exp_int[$j] = "</ul>";
						}		
					}
					$res_exp[$i] = implode('', $res_exp_int);

				}
				else 
				{
					// "ciclo nel mezzo - cycle in the middle"; 
					$res_exp[$i] =  '<li class="msg_err">'. $res_exp[$i];
					$res_exp[$i] = str_replace("</div>", "</li>", $res_exp[$i]);
					$res_exp[$i] = str_replace("<h4>", '<span class="msg"><strong>', $res_exp[$i]);
					$res_exp[$i] = str_replace("</h4>", '</strong></span>', $res_exp[$i]);
					$res_exp[$i] = str_replace("<table>", "<table class='css_error' cellspacing='4px'><tr><th>"._AC('line')."</th><th>"._AC('element')."</th><th>"._AC('error')."</th></tr>", $res_exp[$i]);
					
				}				
			}
			
			$result = implode('', $res_exp);
			
			return $result;
		}
		else
		{
			$this->contain_errors = true;
			$this->msg = '<p class="msg_err">'._AC('AC_ERROR_CSS_VALIDATOR_ERROR').'</p><br/>';
			return false;
		}
	}
	
	/**
	* private
	* return errors/warnings in form of array by striping it out from $this->result
	*/
	function stripOutResultArray()
	{	
		$pattern1 = '/('.preg_quote("<div id='congrats'>", '/').'.*)/s'; // nessun errore -- no errors
		$pattern2 = '/('.preg_quote("<div class='error-section-all'>", '/').'.*)/s'; // when has errors
		$pattern3 = '/('.preg_quote('<p class="backtop"><a href="#banner">&uarr; ', '/').'.*)/s'; // when has errors
		$pattern4 = '/('.preg_quote('<div id="warnings">', '/').'.*)/s'; // when has errors
			
		if (preg_match($pattern1, $this->full_return, $match) || preg_match($pattern2, $this->full_return, $match)|| 
			preg_match($pattern3, $this->full_return, $match) || preg_match($pattern4, $this->full_return, $match)) {		
			
			$pattern_group = '/\<li class="msg_err"\>(.*?)\<\/li\>/s';
			preg_match_all($pattern_group, $this->result, $matches_group);
			foreach($matches_group[1] as $group) {
				unset($group_errors);
				
				$pattern_uri = '/\<span class="msg"\>\<strong\>URI : \<a href="(.*?)"\>(.*?)\<\/a>\<\/strong\>\<\/span\>/s';
				preg_match($pattern_uri, $group, $matches_uri);
				$uri = $matches_uri[1];
				
				$pattern_item = '/\<tr class=(.*?)error(.*?)\>(.*?)\<\/tr\>/s';
				preg_match_all($pattern_item, $group, $matches_item);
			
				foreach($matches_item[3] as $error) {
					unset($error_detail);
					
					// line
					$pattern_line = '/\<td class=(.*?)linenumber(.*?) title=(.*?)Line [0-9]+(.*?)>(.*?)\<\/td\>/';
					preg_match($pattern_line, $error, $matches_line);
					$line = $matches_line[5];
					
					// code
					$pattern_code = '/\<td class=(.*?)codeContext(.*?)>(.*?)\<\/td\>/s';
					if (preg_match($pattern_code, $error, $matches_code)) {
						$code = trim($matches_code[3]);
					} else {
						$code = '';
					}
					
					// parse
					$pattern_parse1 = '/\<td class=(.*?)parse-error(.*?)>(.*?)\<\/td\>/s';
					$pattern_parse2 = '/\<td class=(.*?)invalidparam(.*?)>(.*?)\<\/td\>/s';
					if (preg_match($pattern_parse1, $error, $matches_parse)) {
						$parse = preg_replace("/ {2,}/", " ",str_replace("\n", "", trim($matches_parse[3])));
					} else if (preg_match($pattern_parse2, $error, $matches_parse)) {
						$parse = preg_replace("/ {2,}/", " ",str_replace("\n", "", trim($matches_parse[3])));
					} else {
						$parse = '';
					}			
						
					$error_detail =  array('line' => $line, 'code' => $code, 'parse' => $parse);
					$group_errors[] = $error_detail;
				}
				$this->result_array[$uri] = $group_errors; 
			}
		} else {
			$this->contain_errors = true;
			$this->msg = _AC('AC_ERROR_CSS_VALIDATOR_ERROR');
			return false;
		}	
	}
	
	/**
	* private
	* return number of errors by striping it out from validation output returned from 3rd party
	*/
	function stripOutNumOfErrors($full_result)
	{
		$pattern1 = '/\((\d+)\)/';
		if (preg_match($pattern1, $full_result, $matches)) // match if it returns the number of errors found
		{
			return $matches[1];}
		else
		{
			return 0;
		}
	}
	
	/**
	* public 
	* return validation report in html
	*/
	function getValidationRpt()
	{
		return $this->result;
	}
	
	/**
	* public 
	* return validation report in form of array
	*/
	function getValidationRptArray()
	{
		return $this->result_array;
	}

	// public 
	function getNumOfValidateError()
	{
		return $this->num_of_errors;
	}

	/**
	* public 
	* return error message
	*/
	function getErrorMsg()
	{
		return $this->msg;
	}
	
	/**
	* public 
	* return true or false: if error happens during process
	*/
	function containErrors()
	{
		return $this->contain_errors;
	}
}
?>  