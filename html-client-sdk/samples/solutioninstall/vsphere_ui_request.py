# Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
# Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
# and/or its subsidiaries.

# This is a helper file to facilitate the making of a request to the vSphere Client.
# It receives the vSphere UI endpoint which needs to be contacted,
# the token to verify the request, the vCenter Server thumbprint to validate the
# endpoint and the extension payload.

import sys
import requests
from requests.adapters import HTTPAdapter
from urllib.parse import urlparse

class FingerprintAdapter(HTTPAdapter):
    """
    A TransportAdapter that allows to verify certificates by fingerprint
    """
    def __init__(self, fingerprint, *args, **kwargs):
        self._fingerprint = fingerprint
        HTTPAdapter.__init__(self, *args, **kwargs)

    def init_poolmanager(self, *args, **kwargs):
        kwargs['assert_fingerprint'] = self._fingerprint
        return super().init_poolmanager(*args, **kwargs)

    def proxy_manager_for(self, *args, **kwargs):
        kwargs['assert_fingerprint'] = self._fingerprint
        return super().proxy_manager_for(*args, **kwargs)

url = sys.argv[1]
token = sys.argv[2]
thumbprint = sys.argv[3]
extension = sys.argv[4]

parsed_url = urlparse(url)
host = parsed_url.netloc

session = requests.Session()
session.verify = False
session.mount('https://{}/'.format(host), FingerprintAdapter(thumbprint))
response = session.post(url=url, headers={'Content-Type': 'application/json', 'solution-install-token': token}, data=extension)
print(response)
