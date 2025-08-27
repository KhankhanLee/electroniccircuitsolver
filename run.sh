#!/bin/bash

# 회로 설계 및 분석 마스터 실행 스크립트
echo "회로 설계 및 분석 마스터 - LIKE 유미나이, 세미나이"
echo "================================================"

# Java 버전 확인
echo "Java 버전 확인 중..."
java -version

# 컴파일
echo ""
echo "소스코드 컴파일 중..."
javac CircuitDesigner.java

if [ $? -eq 0 ]; then
    echo "컴파일 성공!"
    echo ""
    echo "프로그램 실행 중..."
    java CircuitDesigner
else
    echo "컴파일 실패! Java JDK가 설치되어 있는지 확인하세요."
    exit 1
fi
