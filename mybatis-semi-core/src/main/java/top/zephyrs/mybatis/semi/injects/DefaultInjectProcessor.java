package top.zephyrs.mybatis.semi.injects;

import top.zephyrs.mybatis.semi.injects.methods.*;

/**
 * 通用方法处理
 */
public class DefaultInjectProcessor extends InjectProcessor {

    @Override
    public void loadMethods() {
        this.addInject(new Insert());
        this.addInject(new UpdateById());
        this.addInject(new DeleteById());

        this.addInject(new SelectAll());
        this.addInject(new SelectById());
        this.addInject(new SelectByQuery());

        this.addInject(new Enable());
        this.addInject(new Disable());
        this.addInject(new ToggleEnable());
    }

}
