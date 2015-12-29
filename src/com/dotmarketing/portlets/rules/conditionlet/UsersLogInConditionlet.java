//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import java.util.Collection;
//import java.util.List;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import static com.dotmarketing.portlets.rules.conditionlet.Comparison.IS;
//
///**
// * This conditionlet will allow dotCMS users to check whether the user that
// * issued the request is currently logged in or not. The login status of a user
// * is in the {@link HttpServletRequest} object, which is used to perform the
// * validation and is retrieved using our own API. This {@link Conditionlet}
// * provides a single drop-down menu with the available comparison mechanisms,
// * and it will check the login status with the back-end system, no other user
// * input is to be required or validated.
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-14-2015
// *
// */
//public class UsersLogInConditionlet extends Conditionlet<UsersLogInConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String CONDITIONLET_NAME = "Current User's Log In Status";
//
//	public UsersLogInConditionlet() {
//        super("api.ruleengine.system.conditionlet.VisitorLoginStatus", ImmutableSet.<Comparison>of(IS,
//                                                                                              Comparison.IS_NOT), parameters);
//	}
//
//
//	@Override
//	public Collection<ConditionletInput> getInputs(String comparisonId) {
//		return null;
//	}
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
////        boolean mustBeLoggedIn = comparison == Comparison.IS;
////		try {
////			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
////			if ((mustBeLoggedIn && user != null)
////					|| (!mustBeLoggedIn && user == null)) {
////				return true;
////			}
////		} catch (DotRuntimeException | PortalException | SystemException e) {
////			Logger.error(this,
////					"Could not retrieved logged-in user from request: "
////							+ request.getRequestURL());
////		}
//		return false;
//	}
//
//    @Override
//    public Instance instanceFrom(Comparison comparison, List<ParameterModel> values) {
//        return new Instance(comparison, values);
//    }
//
//    public static class Instance implements RuleComponentInstance {
//
//        private Instance(Comparison comparison, List<ParameterModel> values) {
//        }
//    }
//
//}
