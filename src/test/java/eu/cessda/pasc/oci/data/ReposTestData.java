package eu.cessda.pasc.oci.data;

import eu.cessda.pasc.oci.models.configurations.Repo;

public class ReposTestData {

  private ReposTestData() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static final Repo getUKDSRepo() {
    Repo repo = new Repo();
    repo.setName("UKDS");
    repo.setUrl("https://oai.ukdataservice.ac.uk:8443/oai/provider");
    return repo;
  }
}