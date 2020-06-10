package eu.cessda.pasc.oci.exception;

import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.Getter;

/**
 * Represents that a repository's handler was not configured.
 */
@Getter
public class HandlerNotFoundException extends IllegalStateException {

    private static final long serialVersionUID = 229579898689610309L;

    /**
     * The repository that caused this exception.
     */
    private final Repo repo;

    /**
     * Constructs a {@link HandlerNotFoundException} with the specified repository.
     *
     * @param repo the repository to construct the message from
     */
    public HandlerNotFoundException(Repo repo) {
        super(String.format("Hander %s for repository %s not configured", repo.getHandler(), repo.getName()));
        this.repo = repo;
    }
}
