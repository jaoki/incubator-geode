FROM centos:centos6

# Basic libraries
RUN yum -y install iputils wget vim git

#Install JAVA
RUN wget --no-check-certificate --no-cookies --header "Cookie:oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u79-b15/jdk-7u79-linux-x64.rpm -O jdk-7u79-linux-x64.rpm && \
	yum -y install jdk-7u79-linux-x64.rpm && \
	rm jdk-7u79-linux-x64.rpm && \
	echo "export JAVA_HOME=/usr/java/default" >> ~/.bashrc && \
	echo "export PATH=$PATH:$JAVA_HOME/bin" >> ~/.bashrc



