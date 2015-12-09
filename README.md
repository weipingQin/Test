# Test
### 2015-12-9
### 写的例子需要集成到正式的app中做相关的集成 
### 编写的一个钥匙状态测试的例子

##### ios那边的处理方式
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


 
