package org.folio.circulationbff.config;

import org.folio.circulationbff.client.CheckInClient;
import org.folio.circulationbff.client.CheckOutClient;
import org.folio.circulationbff.client.CirculationClient;
import org.folio.circulationbff.client.CirculationItemClient;
import org.folio.circulationbff.client.EcsTlrClient;
import org.folio.circulationbff.client.HoldingsStorageClient;
import org.folio.circulationbff.client.InstanceStorageClient;
import org.folio.circulationbff.client.InventoryClient;
import org.folio.circulationbff.client.ItemStorageClient;
import org.folio.circulationbff.client.LoanStorageClient;
import org.folio.circulationbff.client.LoanTypeClient;
import org.folio.circulationbff.client.LocationCampusClient;
import org.folio.circulationbff.client.LocationClient;
import org.folio.circulationbff.client.LocationInstitutionClient;
import org.folio.circulationbff.client.LocationLibraryClient;
import org.folio.circulationbff.client.MaterialTypeClient;
import org.folio.circulationbff.client.RequestMediatedClient;
import org.folio.circulationbff.client.SearchInstancesClient;
import org.folio.circulationbff.client.SearchItemsClient;
import org.folio.circulationbff.client.ServicePointClient;
import org.folio.circulationbff.client.UserClient;
import org.folio.circulationbff.client.UserTenantsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfiguration {

  @Bean
  CirculationClient circulationClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(CirculationClient.class);
  }

  @Bean
  EcsTlrClient ecsTlrClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(EcsTlrClient.class);
  }

  @Bean
  RequestMediatedClient requestMediatedClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(RequestMediatedClient.class);
  }

  @Bean
  CheckInClient checkInClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(CheckInClient.class);
  }

  @Bean
  CheckOutClient checkOutClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(CheckOutClient.class);
  }

  @Bean
  CirculationItemClient circulationItemClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(CirculationItemClient.class);
  }

  @Bean
  InventoryClient inventoryClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(InventoryClient.class);
  }

  @Bean
  ItemStorageClient itemStorageClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(ItemStorageClient.class);
  }

  @Bean
  HoldingsStorageClient holdingsStorageClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(HoldingsStorageClient.class);
  }

  @Bean
  InstanceStorageClient instanceStorageClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(InstanceStorageClient.class);
  }

  @Bean
  LoanStorageClient loanStorageClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(LoanStorageClient.class);
  }

  @Bean
  LoanTypeClient loanTypeClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(LoanTypeClient.class);
  }

  @Bean
  MaterialTypeClient materialTypeClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(MaterialTypeClient.class);
  }

  @Bean
  LocationClient locationClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(LocationClient.class);
  }

  @Bean
  LocationCampusClient locationCampusClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(LocationCampusClient.class);
  }

  @Bean
  LocationInstitutionClient locationInstitutionClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(LocationInstitutionClient.class);
  }

  @Bean
  LocationLibraryClient locationLibraryClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(LocationLibraryClient.class);
  }

  @Bean
  ServicePointClient servicePointClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(ServicePointClient.class);
  }

  @Bean
  SearchInstancesClient searchInstancesClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(SearchInstancesClient.class);
  }

  @Bean
  SearchItemsClient searchItemsClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(SearchItemsClient.class);
  }

  @Bean
  UserClient userClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(UserClient.class);
  }

  @Bean
  UserTenantsClient userTenantsClient(HttpServiceProxyFactory httpServiceProxyFactory) {
    return httpServiceProxyFactory.createClient(UserTenantsClient.class);
  }
}
