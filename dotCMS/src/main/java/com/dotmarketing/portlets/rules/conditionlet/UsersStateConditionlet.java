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
// * This conditionlet will allow CMS users to check the state/province/region a
// * user request comes from. The available options of this conditionlet will be
// * represented as a one or two-character values (depending on the country). This
// * {@link Conditionlet} provides a drop-down menu with the available comparison
// * mechanisms, and a drop-down menu with the states in the USA, where users can
// * select one or more values that will match the selected criterion.
// * <p>
// * The location of the request is determined by the IP address of the client
// * that issued the request. Geographic information is then retrieved via the <a
// * href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java API</a>,
// * which will allow CMS users to display content based on geographic data.
// * </p>
// *
// * @author Jose Castro
// * @version 1.0
// * @since 04-13-2015
// *
// */
//public class UsersStateConditionlet extends Conditionlet<UsersStateConditionlet.Instance> {
//
//	private static final long serialVersionUID = 1L;
//
//	private static final String INPUT_ID = "state";
//	private static final String CONDITIONLET_NAME = "User's State/Province/Region";
//
//	private static final String COMPARISON_IS = "is";
//	private static final String COMPARISON_ISNOT = "isNot";
//
//	private LinkedHashSet<Comparison> comparisons = null;
//	private Map<String, ConditionletInput> inputValues = null;
//
//	public UsersStateConditionlet() {
//        super("api.ruleengine.system.conditionlet.VisitorsStateProvince", ImmutableSet.<Comparison>of(IS, Comparison.IS_NOT), parameters);
//	}
//
//
//	protected ValidationResult validate(Comparison comparison,
//			ConditionletInputValue inputValue) {
//		ValidationResult validationResult = new ValidationResult();
//		String inputId = inputValue.getConditionletInputId();
//		if (UtilMethods.isSet(inputId)) {
//			String selectedValue = inputValue.getValue();
//			if (this.inputValues == null
//					|| this.inputValues.get(inputId) == null) {
//				getInputs(comparison.getId());
//			}
//			ConditionletInput inputField = this.inputValues.get(inputId);
//			validationResult.setConditionletInputId(inputId);
//			Set<EntryOption> inputOptions = inputField.getData();
//			if (inputOptions != null) {
//				for (EntryOption option : inputOptions) {
//					if (option.getId().equals(selectedValue)) {
//						validationResult.setValid(true);
//						break;
//					}
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
//			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
//			ConditionletInput inputField = new ConditionletInput();
//			// Set field configuration and available options
//			inputField.setId(INPUT_ID);
//			inputField.setMultipleSelectionAllowed(true);
//			inputField.setDefaultValue("");
//			inputField.setMinNum(1);
//			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
//			options.add(new EntryOption("AK", "Alaska"));
//			options.add(new EntryOption("AL", "Alabama"));
//			options.add(new EntryOption("AR", "Arkansas"));
//			options.add(new EntryOption("AZ", "Arizona"));
//			options.add(new EntryOption("CA", "California"));
//			options.add(new EntryOption("CO", "Colorado"));
//			options.add(new EntryOption("CT", "Connecticut"));
//			options.add(new EntryOption("DE", "Delaware"));
//			options.add(new EntryOption("FL", "Florida"));
//			options.add(new EntryOption("GA", "Georgia"));
//			options.add(new EntryOption("HI", "Hawaii"));
//			options.add(new EntryOption("IA", "Iowa"));
//			options.add(new EntryOption("ID", "Idaho"));
//			options.add(new EntryOption("IL", "Illinois"));
//			options.add(new EntryOption("IN", "Indiana"));
//			options.add(new EntryOption("KS", "Kansas"));
//			options.add(new EntryOption("KY", "Kentucky"));
//			options.add(new EntryOption("LA", "Louisiana"));
//			options.add(new EntryOption("MA", "Massachusetts"));
//			options.add(new EntryOption("MD", "Maryland"));
//			options.add(new EntryOption("ME", "Maine"));
//			options.add(new EntryOption("MI", "Michingan"));
//			options.add(new EntryOption("MN", "Minnesota"));
//			options.add(new EntryOption("MO", "Missouri"));
//			options.add(new EntryOption("MS", "Mississippi"));
//			options.add(new EntryOption("MT", "Montana"));
//			options.add(new EntryOption("NC", "North Carolina"));
//			options.add(new EntryOption("ND", "North Dakota"));
//			options.add(new EntryOption("NE", "Nebraska"));
//			options.add(new EntryOption("NH", "New Hampshire"));
//			options.add(new EntryOption("NJ", "New Jersey"));
//			options.add(new EntryOption("NM", "New Mexico"));
//			options.add(new EntryOption("AK", "Alaska"));
//			options.add(new EntryOption("NV", "Nevada"));
//			options.add(new EntryOption("NY", "New York"));
//			options.add(new EntryOption("OH", "Ohio"));
//			options.add(new EntryOption("OK", "Oklahoma"));
//			options.add(new EntryOption("OR", "Oregon"));
//			options.add(new EntryOption("PA", "Pennsylvania"));
//			options.add(new EntryOption("RI", "Rhode Island"));
//			options.add(new EntryOption("SC", "South Carolina"));
//			options.add(new EntryOption("SD", "South Dakota"));
//			options.add(new EntryOption("TN", "Tennessee"));
//			options.add(new EntryOption("TX", "Texas"));
//			options.add(new EntryOption("UT", "Utah"));
//			options.add(new EntryOption("VA", "Virginia"));
//			options.add(new EntryOption("VT", "Vermont"));
//			options.add(new EntryOption("WA", "Washington"));
//			options.add(new EntryOption("WI", "Wisconsin"));
//			options.add(new EntryOption("WV", "West Virginia"));
//			options.add(new EntryOption("WY", "Wyoming"));
//			inputField.setData(options);
//			this.inputValues.put(inputField.getId(), inputField);
//		}
//		return this.inputValues.values();
//	}
//
//	@Override
//    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
//
////        GeoIp2CityDbUtil geoIp2Util = GeoIp2CityDbUtil.getInstance();
////		String state = null;
////		try {
////			InetAddress address = HttpRequestDataUtil.getIpAddress(request);
////			String ipAddress = address.getHostAddress();
////			state = geoIp2Util.getSubdivisionIsoCode(ipAddress);
////		} catch (IOException | GeoIp2Exception e) {
////			Logger.error(this,
////					"An error occurred when retrieving the IP address from request: "
////							+ request.getRequestURL());
////		}
////		if (!UtilMethods.isSet(state)) {
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
////			for (ParameterModel value : values) {
////				if (value.getValue().equals(state)) {
////					return true;
////				}
////			}
////		} else if (comparison.getId().equals(COMPARISON_ISNOT)) {
////			for (ParameterModel value : values) {
////				if (value.getValue().equals(state)) {
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
//
//}
