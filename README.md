# mod-circulation-bff

Copyright (C) 2024 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Goal

Module that provides a facade layer (backed-for-frontend) for FOLIO circulation 
applications.

### Environment variables

| Name                          | Default value             | Description                                                                                                                                                                           |
|:------------------------------|:--------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JAVA_OPTIONS                  | -XX:MaxRAMPercentage=66.0 | Java options                                                                                                                                                                          |
| SYSTEM_USER_USERNAME          | mod-circulation-bff       | Username for `mod-circulation-bff` system user                                                                                                                                      |
| SYSTEM_USER_PASSWORD          | mod-circulation-bff       | Password for `mod-circulation-bff` system user                                                                                                         |
| SYSTEM_USER_ENABLED           | true                      | Defines if system user must be created at service tenant initialization                                                                                                               |
| SECURE_TENANT_ID              | -                         | ID of the secure tenant in a consortia-enabled environment                                                                                                                            |
| OKAPI_URL                     | http://okapi:9130         | OKAPI URL used to login system user, required                                                                                                                                         |
| ENV                           | folio                     | The logical name of the deployment, must be unique across all environments using the same shared Kafka/Elasticsearch clusters, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed |

## Further information

### Issue tracker

See project [MCBFF](https://folio-org.atlassian.net/browse/MCBFF)
at the FOLIO issue tracker.