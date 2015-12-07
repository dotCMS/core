//package com.dotmarketing.portlets.rules.conditionlet;
//
//import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
//import com.dotmarketing.portlets.rules.RuleComponentInstance;
//import com.dotmarketing.portlets.rules.ValidationResult;
//import com.dotmarketing.portlets.rules.model.ParameterModel;
//import java.util.Collection;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.dotmarketing.util.UtilMethods;
//
//import static com.dotmarketing.portlets.rules.conditionlet.Comparison.IS;
//
///**
// * This conditionlet will allow CMS users to check the platform a user request
// * comes from, such as, mobile, tablet, desktop, etc. The information is
// * obtained by reading the {@code User-Agent} header in the
// * {@link HttpServletRequest} object. This {@link Conditionlet} provides a
// * drop-down menu with the available comparison mechanisms, and a drop-down menu
// * containing the different platforms that can be detected, where users can
// * select one or more values that will match the selected criterion.
// * <p>
// * The format of the {@code User-Agent} is not standardized (basically free
// * format), which makes it difficult to decipher it. This conditionlet uses a
// * Java API called <a
// * href="http://www.bitwalker.eu/software/user-agent-utils">User Agent Utils</a>
// * which parses HTTP requests in real time and gather information about the user
// * agent, detecting a high amount of browsers, browser types, operating systems,
// * device types, rendering engines, and Web applications.
// * </p>
// *
// * @author Jose Castro
// * @version 1.0
// * @since 05-05-2015
// *
// */
//public class UsersPlatformConditionlet extends Conditionlet<UsersPlatformConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "platform";
//
//	private Map<String, ConditionletInput> inputValues = null;
//
//    public UsersPlatformConditionlet() {
//        super("api.ruleengine.system.conditionlet.VisitorsDeviceType", ImmutableSet.<Comparison>of(IS, Comparison.IS_NOT), parameters);
//    }
//
//	protected ValidationResult validate(Comparison comparison, ConditionletInputValue inputValue) {
//		ValidationResult validationResult = new ValidationResult();
//		String inputId = inputValue.getConditionletInputId();
//		if (UtilMethods.isSet(inputId)) {
//			String selectedValue = inputValue.getValue();
//			String comparisonId = comparison.getId();
//			if (this.inputValues == null || this.inputValues.get(inputId) == null) {
//				getInputs(comparisonId);
//			}
//			ConditionletInput inputField = this.inputValues.get(inputId);
//			validationResult.setConditionletInputId(inputId);
//			Set<EntryOption> inputOptions = inputField.getData();
//			for (EntryOption option : inputOptions) {
//				if (option.getId().equalsIgnoreCase(selectedValue)) {
//					validationResult.setValid(true);
//					break;
//				}
//			}
//			if (!validationResult.isValid()) {
//				validationResult.setErrorMessage("Invalid value for input '"
//						+ inputId + "': '" + selectedValue + "'");
//			}
//		}
//		return validationResult;
//	}
//
//	@Override
//	public Collection<ConditionletInput> getInputs(String comparisonId) {
//		if (this.inputValues == null) {
//			ConditionletInput inputField = new ConditionletInput();
//			inputField.setId(INPUT_ID);
//			inputField.setMultipleSelectionAllowed(true);
//			inputField.setDefaultValue("");
//			inputField.setMinNum(1);
//			Set<EntryOption> options = new LinkedHashSet<>();
//			options.add(new EntryOption("Computer", "Computer"));
//			options.add(new EntryOption("Mobile", "Mobile Device"));
//			options.add(new EntryOption("Tablet", "Tablet"));
//			options.add(new EntryOption("Wearable computer", "Wearable Device"));
//			options.add(new EntryOption("Digital media receiver", "Digital Media Receiver"));
//			options.add(new EntryOption("Game console", "Game Console"));
//			inputField.setData(options);
//			this.inputValues = new LinkedHashMap<>();
//			this.inputValues.put(inputField.getId(), inputField);
//		}
//		return this.inputValues.values();
//	}
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
//
////      String userAgentInfo = request.getHeader("User-Agent");
////		UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
////		String platform = null;
////		if (agent != null && agent.getOperatingSystem() != null) {
////			platform = agent.getOperatingSystem().getDeviceType().getName();
////		}
////		if (!UtilMethods.isSet(platform)) {
////			return false;
////		}
////		Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
////		for (ParameterModel value : values) {
////			inputValues.add(new ConditionletInputValue(INPUT_ID, value
////					.getValue()));
////		}
////		ValidationResults validationResults = validate(comparison, inputValues);
////		if (validationResults.hasErrors()) {
////			return false;
////		}
////		if (comparison.getId().equals(COMPARISON_IS)) {
////			for (ConditionletInputValue input : inputValues) {
////				if (input.getValue().equalsIgnoreCase(platform)) {
////					return true;
////				}
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			for (ConditionletInputValue input : inputValues) {
////				if (input.getValue().equalsIgnoreCase(platform)) {
////					return false;
////				}
////			}
////			return true;
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
//}
