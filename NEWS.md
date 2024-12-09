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
