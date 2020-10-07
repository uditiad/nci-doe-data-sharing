/**
 * DoeAuthorizationService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/blob/master/LICENSE.txt for details.
 */
package gov.nih.nci.doe.web.service;

/**
 * Doe Authorization Service Interface.
 *
 * @author <a href="mailto:mounica.ganta@nih.gov">Mounica Ganta</a>
 */
public interface DoeAuthorizationService {

  /**
   * Authorize user.
   *
   * @param redirectUri The redirectUri.
   * @throws Exception on service failure.
   */
  public String authorize(String redirectUri) throws Exception;
  
  /**
   * Obtain access token using the code.
   *
   * @param code The code.
   * @param redirectUri The redirectUri.
   * @throws Exception on service failure.
   */
  public String getToken(String code, String redirectUri) throws Exception;
  
}
