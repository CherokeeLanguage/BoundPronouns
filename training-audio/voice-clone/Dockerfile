FROM pytorch/pytorch

# Update Ubuntu.
RUN apt-get update
RUN apt-get install -y apt-utils debconf-utils
RUN apt-get upgrade -y
RUN echo 'debconf debconf/frontend select Noninteractive' | debconf-set-selections

#set the locale otherwise weird issues later, especially with git and gradle
RUN apt-get install -y language-pack-en language-pack-en-base
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

#timezone to match mine to prevent confusion in log files
RUN apt-get install -y tzdata
RUN ln -fs /usr/share/zoneinfo/America/New_York /etc/localtime
RUN dpkg-reconfigure -f noninteractive tzdata

RUN apt-get install -y software-properties-common

# Install some necessary tools.
RUN apt-get install -y sudo vim vifm
RUN apt-get install -y perl

# Install dependencies.
RUN apt-get install -y build-essential pkg-config automake libtool cmake
RUN apt-get install -y git
RUN apt-get install -y libboost-all-dev
RUN apt-get install -y libprotobuf\*
RUN apt-get install -y protobuf-compiler
RUN apt-get install -y libprotobuf-dev
RUN apt-get install -y openssl libssl-dev
RUN apt-get install -y libgoogle-perftools-dev
RUN apt-get install -y zlib1g-dev
RUN apt-get install -y python-dev
RUN apt-get install -y libbz2-dev
RUN apt-get install -y wget

#RUN add-apt-repository -y ppa:cwchien/gradle
#RUN apt-get update
#RUN apt-get install -y openjdk-8-jdk-headless
#RUN apt-get install -y gradle

#RUN apt-get install -y cuda

#Newer version of cmake so that Marian-NMT can find CUDA during build.
RUN apt-get update
RUN apt-get install -y apt-transport-https ca-certificates gnupg software-properties-common wget
RUN wget -O - https://apt.kitware.com/keys/kitware-archive-latest.asc 2>/dev/null | apt-key add -
RUN sudo apt-add-repository 'deb https://apt.kitware.com/ubuntu/ bionic main'
RUN apt-get update
RUN apt-get install -y kitware-archive-keyring
RUN apt-key --keyring /etc/apt/trusted.gpg del C1F34CDD40CD72DA
RUN apt-get dist-upgrade -y --autoremove --purge

WORKDIR "/workspace"
RUN apt-get clean \
        && apt-get update \
        && apt-get install -y ffmpeg libportaudio2 openssh-server python3-pyqt5 xauth \
        && apt-get -y autoremove \
        && mkdir /var/run/sshd \
        && mkdir /root/.ssh \
        && chmod 700 /root/.ssh \
        && ssh-keygen -A \
        && sed -i "s/^.*PasswordAuthentication.*$/PasswordAuthentication no/" /etc/ssh/sshd_config \
        && sed -i "s/^.*X11Forwarding.*$/X11Forwarding yes/" /etc/ssh/sshd_config \
        && sed -i "s/^.*X11UseLocalhost.*$/X11UseLocalhost no/" /etc/ssh/sshd_config \
        && grep "^X11UseLocalhost" /etc/ssh/sshd_config || echo "X11UseLocalhost no" >> /etc/ssh/sshd_config
ADD Real-Time-Voice-Cloning/requirements.txt /workspace/requirements.txt
RUN pip install -r /workspace/requirements.txt
RUN echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCx8eaGnt3t/eK882l9UcqMQBfcX6/HDks+J9s2m/aHCLW44XkwY5RlgTebWPv4uAVgR4cRn48Ax/r8N5pNLto8rM8WEF++CBejYTJU6MS3Jks8GhpceOZIAOl43lZPImlBhF8aVTZ1bCftZbnfGPTdu3ca6LutRhu+rTYBX6633IO3wXtBC+32TQ3JvCJ0raPqHmuHHjNcXWHpIzYTpS7yS/uly04FSAOEXsVGAU70cdofk2Ss6TF/t6040Bm9XXyckVEvpZCsJVuqLkmtExXre8ZpTSxaJRHG5o/lapfJLVzku4GaKEu2AsUi7f/LHys5VYm7n4WIwSsMt1fEfb99 mjoyner@Precision-Workstation" \ 
        >> /root/.ssh/authorized_keys
RUN echo "export PATH=/opt/conda/bin:$PATH" >> /root/.profile
ENTRYPOINT ["sh", "-c", "/usr/sbin/sshd && bash"]
# && bash"]
CMD ["bash"]