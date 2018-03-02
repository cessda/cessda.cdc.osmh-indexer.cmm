package eu.cessda.pasc.oci.repository;

import java.util.function.Function;

/**
 * Utility to encode(String replace) known Special Characters in HTTP rest context.
 *
 * This is reversed by the respective handler using the same string replace token here
 * before calling the remote Service providers repo
 *
 * @author moses@doraventures.com
 */
public class StudyIdentifierEncoder {

  static Function<String, String> encodeStudyIdentifier(){
    return studyIdentifier -> studyIdentifier.replace(".", "_dt_")
        .replace("/", "_sl_")
        .replace(":", "_cl_");
  }
}
