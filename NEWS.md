## 1.0.18 2025-05-26
* Populated DCB item fields for circulation loans (MCBFF-102)

## 1.0.17 2025-05-21
* Forward check-out requests in secure tenant to mod-requests-mediated (MCBFF-95)

## 1.0.16 2025-05-07
* Add location and loan type to `search-intances` response (MCBFF-87)
* Fix central tenant check-in for item with hold request (MCBFF-88)
* Close loan in secure tenant upon check-in in central tenant (MCBFF-90)

## 1.0.15 2025-05-06
* [LOC Locate requirement] Allowed Service points should return discovery display name. (MCBFF-93)

## 1.0.14 2025-03-28
* Use holding CN in the check-in slip when item CN is missing (MCBFF-75)

## 1.0.13 2025-03-10
* Proxy check out based on condition (MCBFF-69)

## 1.0.12 2025-03-10
* Add mod-settings permissions to the system user (MCBFF-77)

## 1.0.11 2025-03-07
* Check-out API (MCBFF-68)
* Check in all items from Central tenant (MCBFF-66)

## 1.0.10 2025-02-24
* Rebuild check-in response - item, loan (MCBFF-67)

## 1.0.9 2025-02-21
* Create external mediated requests (MCBFF-46)
* Fetch item details for external request (MCBFF-44)

## 1.0.8 2025-02-03
* Populate staff slip context for non-DCB items (MCBFF-51)
* Update hardcoded effective location during check-in (MCBFF-38)

## 1.0.7 2025-01-28
* Change allowed service point logic (MCBFF-52)

## 1.0.6 2025-01-23
* Fix Allowed SP for non-ECS requests (MCBFF-36)

## 1.0.5 2025-01-21
* Add missing required interfaces (MCBFF-40)

## 1.0.4 2025-01-16
* Check-in API (MCBFF-37)

## 1.0.3 2024-12-12
* Endpoint `create-ecs-request-external` returns primary request (MCBFF-32)

## 1.0.2 2024-12-09
* Failsafe approach to ECS requesting (MCBFF-21)
* Add editions to instance search results (MCBFF-17)

## 1.0.1 2024-11-30

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
