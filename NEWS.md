## 1.1.5 2025-08-27

*

## 1.1.4 2025-08-11

* Tokens are not populated for Due date receipt for Mediated request ([MCBFF-104](https://folio-org.atlassian.net/browse/MCBFF-104))
* ECS | Tokens are not populated for Due date receipt when requested in Central tenant ([MCBFF-102](https://folio-org.atlassian.net/browse/MCBFF-102))

## 1.1.3 2025-06-25

* Implement Declare item lost API (MCBFF-125)
* Refactor schemas, OpenAPI files (MCBFF-18)

## 1.1.2 2025-06-24

* Handle duplicate item IDs in search response (MCBFF-107)
* Forward check-out requests in secure tenant to mod-requests-mediated (MCBFF-95)
* Fix central tenant check-in for item with hold request (MCBFF-88)
* Close loan in secure tenant upon check-in in central tenant (MCBFF-90)

## 1.1.1 2025-04-23

* Add `Location` and `LoanType` fields for `circulation-bff/requests/search-instances` endpoint (MCBFF-87)
* Populate the CN with the Holdings record CN if item's CN is empty (MCBFF-75)

## 1.1.0 2025-03-13

* Proxy check out based on condition (MCBFF-69)
* Add mod-settings permissions to the system user (MCBFF-77)
* Update to Java 21 (MCBFF-81)
* Check-out API (MCBFF-68)
* Check in all items from Central tenant (MCBFF-66)
* Rebuild check-in response - item, loan (MCBFF-67)
* Create external mediated requests (MCBFF-46)
* Fetch item details for external request (MCBFF-44)
* Populate staff slip context for non-DCB items (MCBFF-51)
* Update hardcoded effective location during check-in (MCBFF-38)
* Change allowed service point logic (MCBFF-52)
* Fix Allowed SP for non-ECS requests (MCBFF-36)
* Add missing required interfaces (MCBFF-40)
* Check-in API (MCBFF-37)
* Endpoint `create-ecs-request-external` returns primary request (MCBFF-32)
* Failsafe approach to ECS requesting (MCBFF-21)
* Add editions to instance search results (MCBFF-17)
* Fix staff slip token name (MCBFF-27)
* Pass patron group to allowed SP call for non-central ECS case (MCBFF-25)
* Add `tlr-settings` interface dependency (MCBFF-26)
* Add system user to module descriptor (MCBFF-24)
* Make mod-tlr dependency optional, fix allowed SP routing (MCBFF-20)
* Make `pickupServicePointId` field non-required for POST `/circulation-bff/requests` endpoint (MCBFF-19)
* Add new endpoints to proxy to old or new slip endpoints (MCBFF-12)
* Add excludes config `api-doc`
* Increase minor version, fix typo requestLevel (MCBFF-6)
* Add endpoint to resolve external user IDs (MCBFF-7)
* Update folio-spring-support to 8.2 (MCBFF-15)

## 1.0.0 2024-11-01

* Add `hrid` parameter to request schema (MCBFF-13)
* Search Instances API (MCBFF-4)
* Create endpoint for mediated request Save & Confirm (MCBFF-10)
* Proxy instance search calls to mod-search (MCBFF-3)
* Enable BFF for allowed service points functionality (MCBFF-5)
* Create a skeleton endpoint for mod-search facade (MCBFF-2)
* Implement `tenant` API (MCBFF-8)
* Enable GitHub Workflows `api-lint`, `api-schema-lint`, and `api-doc` (FOLIO-3678)
* Add initial files (MCBFF-1)
