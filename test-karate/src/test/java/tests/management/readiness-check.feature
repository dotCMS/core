Feature: Readiness Check Helper

  Scenario: Check if readiness endpoint is ready
    * def readinessUrl = __arg.readinessUrl
    Given url readinessUrl
    When method get 