FROM centos:centos6

# Basic libraries
RUN yum -y install iputils wget vim git

#Install JAVA
RUN wget --no-check-certificate --no-cookies --header "Cookie:oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u79-b15/jdk-7u79-linux-x64.rpm -O jdk-7u79-linux-x64.rpm
RUN yum -y install jdk-7u79-linux-x64.rpm
RUN rm jdk-7u79-linux-x64.rpm
RUN echo "export JAVA_HOME=/usr/java/default" >> ~/.bashrc
RUN echo "export PATH=$PATH:$JAVA_HOME/bin" >> ~/.bashrc



