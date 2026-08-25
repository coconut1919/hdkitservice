FROM swr.cn-south-1.myhuaweicloud.com/cloud_devstage/hd-space-ai-base2:20260812

ARG DEVBRIDGE_VERSION=0.1.13-release
ARG DEVBRIDGE_BASE_URL=https://res-hd.hc-cdn.cn/sharedata/hdspace/devbridge
ARG DEVBRIDGE_SHA256=ced1d7de5947b077ef0d9bef80ac70b53645f08e516fbeff47156d88b2914dd7

ARG NODE_VERSION=22.23.2
ARG NODE_DIST_URL=https://nodejs.org/dist
ARG NODE_SHA256=fff4078c5def658577f92c88db7db3bc0072924bfb93fe52c1e744a54e94abb8

ARG HWCLOUD_URL=https://res-hd.hc-cdn.cn/sharedata/hdspace/agent/hwcloud/hwcloud-linux-arm64.tar.gz
ARG HWCLOUD_SHA256=28471b196c5a60fd15e61ea7f6af262b017176de8d5359619505381f849996c6

ARG OPENAGENT_VERSION=v0.0.1-alpha.13
ARG OPENAGENT_BASE_URL=https://gitcode.com/huawei-developers/metadata/releases/download
ARG OPENAGENT_SHA256=0a06838cda1496d26c899f80415de2311bb2608952b0e29a96d73ea000048c6b

RUN curl -fsSL -o /tmp/devbridge \
      "${DEVBRIDGE_BASE_URL}/devbridge_Linux_arm64_${DEVBRIDGE_VERSION}" \
    && echo "${DEVBRIDGE_SHA256}  /tmp/devbridge" | sha256sum -c - \
    && install -m 0755 /tmp/devbridge /usr/local/bin/devbridge \
    && rm -f /tmp/devbridge \
    && devbridge version

RUN curl -fsSL -o /tmp/node.tar.xz \
      "${NODE_DIST_URL}/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-arm64.tar.xz" \
    && echo "${NODE_SHA256}  /tmp/node.tar.xz" | sha256sum -c - \
    && rm -rf /usr/local/nodejs \
    && mkdir -p /usr/local/nodejs \
    && tar -xf /tmp/node.tar.xz -C /usr/local/nodejs --strip-components=1 \
    && rm -f /tmp/node.tar.xz \
    && node --version

RUN curl -fsSL -o /tmp/hcloud_install.sh \
      https://hwcloudcli.obs.cn-north-1.myhuaweicloud.com/cli/latest/hcloud_install.sh \
    && bash /tmp/hcloud_install.sh -y \
    && rm -f /tmp/hcloud_install.sh \
    && hcloud version

RUN curl -fsSL -o /tmp/hwcloud.tar.gz "${HWCLOUD_URL}" \
    && echo "${HWCLOUD_SHA256}  /tmp/hwcloud.tar.gz" | sha256sum -c - \
    && tar -xzf /tmp/hwcloud.tar.gz -C /tmp \
    && install -m 0755 /tmp/hwcloud-linux-arm64 /usr/local/bin/hwcloud \
    && rm -f /tmp/hwcloud.tar.gz /tmp/hwcloud-linux-arm64 \
    && hwcloud version

RUN curl -fsSL -o /tmp/openagent.zip \
      "${OPENAGENT_BASE_URL}/${OPENAGENT_VERSION}/release_${OPENAGENT_VERSION}.zip" \
    && echo "${OPENAGENT_SHA256}  /tmp/openagent.zip" | sha256sum -c - \
    && mkdir -p /tmp/openagent \
    && python3 -c "import zipfile; zipfile.ZipFile('/tmp/openagent.zip').extract('release/arm/cli', '/tmp/openagent')" \
    && install -m 0755 /tmp/openagent/release/arm/cli /usr/local/bin/openagent-cli \
    && rm -rf /tmp/openagent /tmp/openagent.zip \
    && openagent-cli --version

RUN dnf install -y nginx net-tools \
    && dnf clean all \
    && nginx -v

RUN corepack enable \
    && corepack prepare pnpm@9 --activate \
    && pnpm --version
