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

define('AC_INCLUDE_PATH', '../include/');
include_once(AC_INCLUDE_PATH.'vitals.inc.php');
include_once(AC_INCLUDE_PATH.'classes/DAO/ChecksDAO.class.php');
include_once(AC_INCLUDE_PATH.'classes/DAO/CheckPrerequisitesDAO.class.php');
include_once(AC_INCLUDE_PATH.'classes/DAO/TestPassDAO.class.php');
include_once(AC_INCLUDE_PATH.'classes/DAO/CheckExamplesDAO.class.php');
include_once(AC_INCLUDE_PATH.'classes/DAO/GuidelinesDAO.class.php');
include_once(AC_INCLUDE_PATH.'classes/DAO/UsersDAO.class.php');

if (isset($_GET['id'])) $check_id = $_GET['id'];
$checkPrerequisitesDAO = new CheckPrerequisitesDAO();
$testPassDAO = new TestPassDAO();
$guidelinesDAO = new GuidelinesDAO();
$checkExamplesDAO = new CheckExamplesDAO();

// handle submit
if (isset($_POST['cancel'])) 
{
	header('Location: index.php');
	exit;
} 
// check on isset($_POST['html_tag']) is to handle javascript submit request for unsaved changes
else if (isset($_POST['save_no_close']) || isset($_POST['save_and_close']) || $_POST['javascript_submit']) 
{
	$checksDAO = new ChecksDAO();
	
	if (!isset($check_id))  // create new user
	{
		$check_id = $checksDAO->Create($_SESSION['user_id'],
                  $_POST['html_tag'],$_POST['confidence'],$_POST['note'],
		          $_POST['name'],$_POST['err'],$_POST['description'],$_POST['search_str'],
		          $_POST['long_description'],$_POST['rationale'],$_POST['how_to_repair'],
		          $_POST['repair_example'],$_POST['question'],$_POST['decision_pass'],
		          $_POST['decision_fail'],$_POST['test_procedure'],$_POST['test_expected_result'],
		          $_POST['test_failed_result'],$_POST['open_to_public']);
	}
	else  // edit existing check
	{
		$checksDAO->Update($check_id, $_SESSION['user_id'],
                  $_POST['html_tag'],$_POST['confidence'],$_POST['note'],
		          $_POST['name'],$_POST['err'],$_POST['description'],$_POST['search_str'],
		          $_POST['long_description'],$_POST['rationale'],$_POST['how_to_repair'],
		          $_POST['repair_example'],$_POST['question'],$_POST['decision_pass'],
		          $_POST['decision_fail'],$_POST['test_procedure'],$_POST['test_expected_result'],
		          $_POST['test_failed_result'],$_POST['open_to_public']);
	}
	
	if (!$msg->containsErrors())
	{
		// re-create check examples
		$checkExamplesDAO->DeleteByCheckID($check_id);
		
		$pass_example_desc = trim($_POST['pass_example_desc']);
		$pass_example = trim($_POST['pass_example']);
		$fail_example_desc = trim($_POST['fail_example_desc']);
		$fail_example = trim($_POST['fail_example']);
		
		if ($pass_example_desc <> '' || $pass_example <> '')
			$checkExamplesDAO->Create($check_id, AC_CHECK_EXAMPLE_PASS, $pass_example_desc, $pass_example);

		if ($fail_example_desc <> '' || $fail_example <> '')
			$checkExamplesDAO->Create($check_id, AC_CHECK_EXAMPLE_FAIL, $fail_example_desc, $fail_example);
			
		$msg->addFeedback('ACTION_COMPLETED_SUCCESSFULLY');
		
		if (isset($_POST['save_and_close']))
		{
			header('Location: index.php');
		}
		else
		{
			header('Location: '.$_SERVER['PHP_SELF'].'?id='.$check_id);
		}
		exit;
	}
}
else if (isset($_POST['remove_pre']))
{
	if (is_array($_POST['del_pre_checks_id']))
	{
		foreach ($_POST['del_pre_checks_id'] as $del_check_id)
			$checkPrerequisitesDAO->Delete($check_id, $del_check_id);
	}
}
else if (isset($_POST['remove_next']))
{
	if (is_array($_POST['del_next_checks_id']))
	{
		foreach ($_POST['del_next_checks_id'] as $del_check_id)
			$testPassDAO->Delete($check_id, $del_check_id);
	}
}
// end of handle submit

// initialize page 
$checksDAO = new ChecksDAO();

if (isset($check_id)) // edit existing user
{
	$check_row = $checksDAO->getCheckByID($check_id);
	
	if (!$check_row)
	{ // invalid check id
		$msg->addError('INVALID_CHECK_ID');
		require(AC_INCLUDE_PATH.'header.inc.php');
		$msg->printAll();
		require(AC_INCLUDE_PATH.'footer.inc.php');
		exit;
	}
	
	// get author name
	$usersDAO = new UsersDAO();
	$user_name = $usersDAO->getUserName($check_row['user_id']);

	if ($user_name <> '') $savant->assign('author', $user_name);

	$check_pass_example_rows = $checkExamplesDAO->getByCheckIDAndType($check_id, AC_CHECK_EXAMPLE_PASS);
	$check_fail_example_rows = $checkExamplesDAO->getByCheckIDAndType($check_id, AC_CHECK_EXAMPLE_FAIL);
	$check_example_row['pass_example_desc'] = $check_pass_example_rows[0]['description'];
	$check_example_row['pass_example'] = $check_pass_example_rows[0]['content'];
	$check_example_row['fail_example_desc'] = $check_fail_example_rows[0]['description'];
	$check_example_row['fail_example'] = $check_fail_example_rows[0]['content'];
	
	$savant->assign('check_row', $check_row);
	$savant->assign('pre_rows', $checkPrerequisitesDAO->getPreChecksByCheckID($check_id));
	$savant->assign('next_rows', $testPassDAO->getNextChecksByCheckID($check_id));
	$savant->assign('guideline_rows', $guidelinesDAO->getEnabledGuidelinesByCheckID($check_id));
	$savant->assign('check_example_row', $check_example_row);
}

/*****************************/
/* template starts down here */

$savant->display('check/check_create_edit.tmpl.php');

?>