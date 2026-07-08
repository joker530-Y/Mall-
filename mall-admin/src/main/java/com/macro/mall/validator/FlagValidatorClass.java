package com.macro.mall.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 状态约束校验器
 * Created by macro on 2018/4/26.
 */
public class FlagValidatorClass implements ConstraintValidator<FlagValidator,Integer> {
    /** 允许的状态值数组 */
    private String[] values;

    /**
     * 初始化校验器，提取注解中的允许值
     * @param flagValidator FlagValidator注解
     */
    @Override
    public void initialize(FlagValidator flagValidator) {
        this.values = flagValidator.value();
    }

    /**
     * 校验给定的值是否在允许的范围内
     * @param value 待校验的值
     * @param constraintValidatorContext 约束校验上下文
     * @return true表示校验通过
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = false;
        if(value==null){
            //当状态为空时使用默认值
            return true;
        }
        for(int i=0;i<values.length;i++){
            if(values[i].equals(String.valueOf(value))){
                isValid = true;
                break;
            }
        }
        return isValid;
    }
}
