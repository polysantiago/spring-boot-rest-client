package io.github.polysantiago.spring.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.Module;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class RestClientHateoasTest {

  private static final String SINGLE_RESOURCE = "/foo/{id}";
  private static final String COLLECTION_RESOURCE = "/foos";
  private static final String OPTIONAL_COLLECTION_RESOURCE = "/foosOptional";
  private static final String PAGED_RESOURCE = "/foosPaged";

  private PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("{", "}");

  @Autowired private BarClient barClient;
  @Autowired private RestTemplate restTemplate;

  @Value("foo_resource.json")
  private ClassPathResource fooResourceJson;

  @Value("foo_resources.json")
  private ClassPathResource fooResourcesJson;

  @Value("foo_pagedResources.json")
  private ClassPathResource fooPagedResourcesJson;

  private MockRestServiceServer server;

  @Before
  public void setUp() {
    server = createServer(restTemplate);
  }

  @After
  public void tearDown() {
    server.verify();
  }

  @Test
  public void testSingleResource() {
    String endpoint = helper.replacePlaceholders(SINGLE_RESOURCE, s -> "1234");

    mockServerHalResponse(endpoint, fooResourceJson);

    FooResource foo = barClient.getFoo("1234");

    assertThat(foo).isNotNull();
    assertThat(foo.getId()).isNotNull();
    assertThat(foo.getLink("foo").getHref()).isNotEmpty();
    assertThat(foo.getBar()).isEqualTo("some-value");
  }

  @Test
  public void testSingleResourceWrapped() {
    String endpoint = helper.replacePlaceholders(SINGLE_RESOURCE, s -> "1234");

    mockServerHalResponse(endpoint, fooResourceJson);

    Resource<Foo> resource = barClient.getFooWrapped("1234");

    assertThat(resource).isNotNull();
    assertThat(resource.getId()).isNotNull();
    assertThat(resource.getLink("foo").getHref()).isNotEmpty();
    assertThat(resource.getContent()).extracting(Foo::getBar).isEqualTo("some-value");
  }

  @Test
  public void testCollectionResource() {
    mockServerHalResponse(COLLECTION_RESOURCE, fooResourcesJson);

    Resources<FooResource> foos = barClient.getFoos();

    assertThat(foos.getId()).isNotNull();
    assertThat(foos.getContent()).containsExactly(new FooResource("some-value"));
  }

  @Test
  public void testOptionalCollectionResource_notFound() {
    mockServerHalResponse(OPTIONAL_COLLECTION_RESOURCE)
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Optional<Resources<FooResource>> optionalFoos = barClient.getOptionalFoos();

    assertThat(optionalFoos).isNotPresent();
  }

  @Test
  public void testOptionalCollectionResource_found() {
    mockServerHalResponse(OPTIONAL_COLLECTION_RESOURCE, fooResourcesJson);

    Optional<Resources<FooResource>> optionalFoos = barClient.getOptionalFoos();

    assertThat(optionalFoos)
        .hasValueSatisfying(
            resources ->
                assertThat(resources)
                    .first()
                    .isEqualToComparingOnlyGivenFields(new Foo("some-value"), "bar"));
  }

  @Test
  public void testPagedCollectionResource() {
    mockServerHalResponse(PAGED_RESOURCE, fooPagedResourcesJson);

    PagedResources<FooResource> pagedFoos = barClient.getPagedFoos();

    assertThat(pagedFoos).isNotEmpty();
    assertThat(pagedFoos.getContent()).isNotEmpty();
    assertThat(pagedFoos.getMetadata()).extracting(PageMetadata::getTotalElements).isEqualTo(1L);
  }

  private void mockServerHalResponse(String endpoint, org.springframework.core.io.Resource reply) {
    mockServerHalResponse(endpoint).andRespond(withSuccess(reply, MediaTypes.HAL_JSON));
  }

  private ResponseActions mockServerHalResponse(String endpoint) {
    return server
        .expect(requestTo("http://localhost" + endpoint))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE));
  }

  @RestClient(value = "bar-client", url = "${localhost.uri}")
  interface BarClient {

    @GetMapping(value = SINGLE_RESOURCE, produces = MediaTypes.HAL_JSON_VALUE)
    FooResource getFoo(@PathVariable("id") String id);

    @GetMapping(value = SINGLE_RESOURCE, produces = MediaTypes.HAL_JSON_VALUE)
    Resource<Foo> getFooWrapped(@PathVariable("id") String id);

    @GetMapping(value = COLLECTION_RESOURCE, produces = MediaTypes.HAL_JSON_VALUE)
    Resources<FooResource> getFoos();

    @GetMapping(value = OPTIONAL_COLLECTION_RESOURCE, produces = MediaTypes.HAL_JSON_VALUE)
    Optional<Resources<FooResource>> getOptionalFoos();

    @GetMapping(value = PAGED_RESOURCE, produces = MediaTypes.HAL_JSON_VALUE)
    PagedResources<FooResource> getPagedFoos();
  }

  @Configuration
  @EnableAutoConfiguration
  @EnableRestClients(basePackageClasses = BarClient.class)
  @EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
  static class ContextConfiguration {

    @Bean
    Module halModule() {
      return new Jackson2HalModule();
    }
  }
}
