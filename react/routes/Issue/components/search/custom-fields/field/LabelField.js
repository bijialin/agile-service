import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { unionBy } from 'lodash';
import SelectFocusLoad from '@/components/SelectFocusLoad';
import { configTheme } from '@/common/utils';

let list = [];
function LabelField({ field, value, onChange }) {
  const [, setValue] = useState(0);
  return (
    <SelectFocusLoad
      {...configTheme({
        list,
        textField: 'labelName',
        valueFiled: 'labelId',
        parseNumber: true,
      })}
      type="label_id"
      loadWhenMount
      style={{ width: 120, margin: 0 }}
      mode="multiple"
      showCheckAll={false}
      allowClear
      dropdownMatchSelectWidth={false}
      placeholder={field.name}
      saveList={(v) => {
        const shouldRender = list.length === 0 && value && value.length > 0;
        list = unionBy(list, v, 'labelId'); 
        // 已保存筛选条件含有用户，并且这个时候select并没有显示，那么选了自定义筛选，要渲染一次
        if (list.length > 0 && shouldRender) {
          setValue(Math.random());
        }
      }}
      filter
      onChange={onChange}
      value={value}
      getPopupContainer={triggerNode => triggerNode.parentNode}    
      requestArgs={[]}
    />
  );
}
export default observer(LabelField);