/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.gateway;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import jakarta.xml.ws.BindingProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.vmware.sample.remote.configuration.Configuration;
import com.vmware.sample.remote.exception.RemotePluginException;
import com.vmware.sample.remote.util.CertificateUtil;
import com.vmware.sample.remote.vim25.ssl.ThumbprintTrustManager;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Implementation that uses the clone session REST API to acquire a new session from VPXD
 * </p>
 * <p>
 * To acquire a cloned session the {@link SessionServiceImpl} follows these steps:
 * <ul>
 * <li>1. Contact the API-GW to acquire a clone ticket for the VPX session</li>
 * <li>2. Using the clone ticket clone the session</li>
 * <li>3. Cache the stored session for a certain amount of time</li>
 * </ul>
 * </p>
 * <p>
 * Maintains a {@link LoadingCache} with all the {@link VimPortType}s that have already
 * been cloned, based on a combination of vCenter Server GUID and session ID. The session
 * lazy loads data per each vCenter Server and per each session. When a value from the
 * cache is removed the manager attempts to terminate the associated cloned session
 * </p>
 */
public class SessionServiceImpl implements SessionService {
   private static final Log logger = LogFactory
         .getLog(SessionServiceImpl.class);
   private static final VimService vimService = new VimService();
   // The first call to VimService::getVimPort() is taking a lot of time.
   // This dummy initialization is done once on deploy time, in order to avoid delay when
   // a user is interacting with the plugin.
   private static final VimPortType vimPort = vimService.getVimPort();

   // https://kpavlov.me/blog/jax-ws-with-custom-sslsocketfactory/
   private static final String SSL_SOCKET_FACTORY = "com.sun.xml.ws.transport.https.client.SSLSocketFactory";

   private static final String CLONE_TICKET_PATH = "/vcenter/session/clone-ticket";
   private static final String VCENTER_GUID_PROP = "vc_guid";

   private final ManagedObjectReference sessionManager;
   private final Configuration configurationService;

   public SessionServiceImpl(final Configuration configurationService) {
      this.configurationService = configurationService;
      sessionManager = new ManagedObjectReference();
      sessionManager.setValue("SessionManager");
      sessionManager.setType("SessionManager");
   }

   private final RemovalListener<VcenterInfo, VimSessionInfo> removalListener = new SessionRemovalListener();
   private final CacheLoader<VcenterInfo, VimSessionInfo> loader = new SessionCacheLoader();
   private final LoadingCache<VcenterInfo, VimSessionInfo> sessionCache = CacheBuilder
         .newBuilder().maximumSize(100).expireAfterAccess(10, TimeUnit.MINUTES)
         .removalListener(removalListener).build(loader);
   private final ObjectMapper objectMapper = new ObjectMapper();

   private RestTemplate buildRestTemplate() {
      final SSLContext sslContext;
      try {
         final TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates,
                  String s) {
               return CertificateUtil.isThumbprintTrusted(x509Certificates[0],
                     configurationService.getVcenterSslThumbprint());
            }
         };
         sslContext = SSLContexts.custom()
               .loadTrustMaterial(null, acceptingTrustStrategy).build();
      } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
         throw new RuntimeException("Failed to build an SSL context", e);
      }

      final SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
            sslContext, NoopHostnameVerifier.INSTANCE);
      final CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(csf).build();

      final HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory();
      requestFactory.setHttpClient(httpClient);

      final RestTemplate restTemplate = new RestTemplate(requestFactory);
      return restTemplate;
   }

   /**
    * Returns a new {@link HttpEntity} and sets the correct headers
    * for the specific vCenter Server.
    *
    * @param vcenterGuid the vCenter Server Guid.
    */
   private HttpEntity<?> getHttpRequestEntity(final String sessionId,
         final String vcenterGuid) {
      final HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set(GatewayCredentials.API_SESSION_ID_NAME, sessionId);
      final ObjectNode node = objectMapper.createObjectNode();
      node.put(VCENTER_GUID_PROP, vcenterGuid);
      return new HttpEntity<>(node, headers);
   }

   public VimSessionInfo getVimSessionInfo() {
      final HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.currentRequestAttributes()).getRequest();
      final GatewayCredentials credentials = GatewayCredentials
            .fromRequestHeaders(request);
      return getVimSessionInfo(credentials);
   }

   public VimSessionInfo getVimSessionInfo(
         final GatewayCredentials credentials) {

      final VcenterInfo vcenterInfo = new VcenterInfo(
            credentials.sessionId,
            configurationService.getVcenterGuid());
      try {
         return this.sessionCache.get(vcenterInfo);
      } catch (final UncheckedExecutionException e) {
         logger.error(String.format("Error connecting vCenter Server. "
                     + "Please check the parameters you provided are correct: "
                     + "\nvcenter.fqdn=%s\n vcenter.guid=%s"
                     + "\nvcenter.thumbprint=%s\nvcenter.port=%s",
               configurationService.getVcenterServerFqdn(),
               configurationService.getVcenterGuid(),
               configurationService.getVcenterSslThumbprint(),
               configurationService.getVcenterServerPort()), e);
         throw new RemotePluginException("errors.vcenterConnectivity");
      } catch (final Exception e) {
         logger.error(
               "An unexpected error occured. Please check the logs for detailed information");
         throw new RuntimeException(e);
      }
   }

   /**
    * "Loads" the session information by first extracting a clone ticket from
    * the vCenter Server and then using {@link VimPortType} clone the ticket
    */
   public class SessionCacheLoader
         extends CacheLoader<VcenterInfo, VimSessionInfo> {
      @Override
      public VimSessionInfo load(final VcenterInfo key)
            throws URISyntaxException, InvalidLoginFaultMsg,
            RuntimeFaultFaultMsg, KeyManagementException,
            NoSuchAlgorithmException {
         // Step 1. Acquire a clone ticket from the API-GW of the server
         // where the plugin is registered
         final RestTemplate restTemplate = buildRestTemplate();
         final String uri = UriComponentsBuilder
               .fromHttpUrl(configurationService.getVcenterRestEndpoint())
               .path(CLONE_TICKET_PATH).toUriString();
         final CloneSessionReply cloneTicket = restTemplate
               .exchange(uri, HttpMethod.POST,
                     getHttpRequestEntity(key.sessionId, key.vcGuid),
                     CloneSessionReply.class).getBody();
         final VimPortType vimPort = vimService.getVimPort();
         final Map<String, Object> context = ((BindingProvider) vimPort)
               .getRequestContext();
         context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
               configurationService.getVcenterApiEndpoint().toString());
         context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
         context.put(SSL_SOCKET_FACTORY, getSSLSocketFactory());
         // Step 2. Contact the SessionManager in VPX (the one which the plugin
         // server is registered against) to build up a session using the
         // acquired clone ticket.
         // The calling server may differ from the one the plugin server
         // is registered against (e.g. in HLM mode). That is why we use
         // the configured vcenter.address to build the sdk's URL, instead of
         // relying on the sdk's URL provided by the calling vCenter Server.
         final UserSession userSession = vimPort.cloneSession(sessionManager, cloneTicket.sessionCloneTicket);
         return new VimSessionInfo(vimPort, userSession);
      }
   }

   /**
    * {@link RemovalListener} that terminates the session on a dedicated {@link VimPortType}
    */
   public class SessionRemovalListener
         implements RemovalListener<VcenterInfo, VimSessionInfo> {
      @Override
      public void onRemoval(
            final RemovalNotification<VcenterInfo, VimSessionInfo> notification) {
         try {
            notification.getValue().getVimPort().logout(sessionManager);
         } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
            logger.info("The session is already destroyed.");
         }
      }
   }

   /**
    * Creates a socket factory for TLS/SSL connection.
    * It is used to validate the identity of the plugin server against the
    * vCenter APIs.
    */
   private SSLSocketFactory getSSLSocketFactory()
         throws NoSuchAlgorithmException, KeyManagementException {
      TrustManager[] trustManagers = new TrustManager[1];
      TrustManager tm = new ThumbprintTrustManager(
            configurationService.getVcenterSslThumbprint());
      trustManagers[0] = tm;

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, null);

      SSLSessionContext sslsc = sslContext.getServerSessionContext();
      sslsc.setSessionTimeout(0);

      return sslContext.getSocketFactory();
   }

   public static class VcenterInfo {
      private final String sessionId;
      private final String vcGuid;

      VcenterInfo(final String sessionId, final String vcGuid) {
         if (sessionId == null || vcGuid == null) {
            throw new AssertionError("Invalid values provided");
         }

         this.sessionId = sessionId;
         this.vcGuid = vcGuid;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         VcenterInfo that = (VcenterInfo) o;
         return sessionId.equals(that.sessionId) &&
               vcGuid.equals(that.vcGuid);
      }

      @Override
      public int hashCode() {
         return Objects.hash(sessionId, vcGuid);
      }
   }
}
