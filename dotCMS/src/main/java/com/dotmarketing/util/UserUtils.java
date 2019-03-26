package com.dotmarketing.util;

import com.dotmarketing.factories.ChallengeQuestionFactory;
import java.util.List;

/**
 * This class was created to manage the challenge question search to db
 *
 * @author Martin Amaris
 * @author Oswaldo Gallango
 */
public class UserUtils {

  /**
   * This method return a list of challenge question list
   *
   * @return List
   */
  public static List getChallengeQuestionList() {

    return ChallengeQuestionFactory.getChallengeQuestionList();
  }
}
