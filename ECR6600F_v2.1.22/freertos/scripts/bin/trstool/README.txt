//����pem��ʽ˽Կ
./secure_tool generate_signing_key key.pem    

//��˽Կ���ɹ�Կ
./secure_tool extract_public_key --keyfile key.pem pub.key    

//�ù�Կ��֤�Ѿ�ǩ�����ļ�
./secure_tool verify_signature --keyfile pub.key 1_sign.bin    