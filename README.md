# Test
### 2015-12-9
### д��������Ҫ���ɵ���ʽ��app������صļ��� 
### ��д��һ��Կ��״̬���Ե�����

##### ios�ǱߵĴ���ʽ
##### if (check.backupKeyIds.count == 0) {
                        key.hasbackup = NO;
                    }
                    else {
                            if ([check.backupKeyIds containsObject:[NSNumber numberWithInt:key.kid]]) {
                                if ([keyIdArr containsObject:[NSNumber numberWithInt:key.kid]]) {
                                    key.hasbackup = YES;
                                }
                                else {
                                    key.hasbackup = NO;
                                }
                            }
                            else {
                            key.hasbackup = NO;
                            }
                    }


 
