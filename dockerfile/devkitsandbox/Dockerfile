FROM swr.cn-south-1.myhuaweicloud.com/cloud_devstage/hd-space-ai-base2:20260812

ARG DEVBRIDGE_VERSION=0.1.13-release
ARG DEVBRIDGE_BASE_URL=https://res-hd.hc-cdn.cn/sharedata/hdspace/devbridge
ARG DEVBRIDGE_SHA256=ced1d7de5947b077ef0d9bef80ac70b53645f08e516fbeff47156d88b2914dd7

RUN curl -fsSL -o /tmp/devbridge \
      "${DEVBRIDGE_BASE_URL}/devbridge_Linux_arm64_${DEVBRIDGE_VERSION}" \
    && echo "${DEVBRIDGE_SHA256}  /tmp/devbridge" | sha256sum -c - \
    && install -m 0755 /tmp/devbridge /usr/local/bin/devbridge \
    && rm -f /tmp/devbridge \
    && devbridge version
