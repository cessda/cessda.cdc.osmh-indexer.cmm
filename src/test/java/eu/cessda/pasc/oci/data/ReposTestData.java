package eu.cessda.pasc.oci.data;

import eu.cessda.pasc.oci.models.configurations.Endpoints;
import eu.cessda.pasc.oci.models.configurations.Repo;

import java.util.Arrays;

public class ReposTestData {

  private ReposTestData() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static Repo getUKDSRepo() {
    Repo repo = new Repo();
    repo.setName("UKDS");
    repo.setUrl("https://oai.ukdataservice.ac.uk:8443/oai/provider");
    repo.setHandler("http://localhost:9091");
    return repo;
  }

  public static Repo getGesisEnRepo() {
    Repo repo = new Repo();
    repo.setName("GESIS");
    repo.setUrl("https://dbk.gesis.org/dbkoai");
    repo.setHandler("http://localhost:9091");
    return repo;
  }

  public static Repo getGesisDeRepo() {
    Repo repo = new Repo();
    repo.setName("GESIS De");
    repo.setUrl("https://dbk.gesis.org/dbkoai/");
    repo.setHandler("http://localhost:9091");
    return repo;
  }

  public static Endpoints getEndpoints() {
    Endpoints endpoints = new Endpoints();
    endpoints.setRepos(Arrays.asList(getUKDSRepo()));
    return endpoints;
  }
}