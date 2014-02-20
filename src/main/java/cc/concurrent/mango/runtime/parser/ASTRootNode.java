package cc.concurrent.mango.runtime.parser;


import cc.concurrent.mango.runtime.ParsedSql;
import cc.concurrent.mango.runtime.RuntimeContext;
import cc.concurrent.mango.runtime.TypeContext;
import cc.concurrent.mango.util.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * 抽象语法树根节点
 *
 * @author ash
 */
public class ASTRootNode extends SimpleNode {

    public ASTRootNode(int i) {
        super(i);
    }

    public ASTRootNode(Parser p, int i) {
        super(p, i);
    }

    /**
     * 检测节点类型
     *
     * @param context
     */
    public void checkType(TypeContext context) {
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node node = jjtGetChild(i);
            if (node instanceof ValuableNode) {
                ((ValuableNode) node).checkType(context);
            }
        }
    }

    /**
     * 获得可迭代参数节点
     *
     * @return
     */
    public List<ASTIterableParameter> getASTIterableParameters() {
        List<ASTIterableParameter> aips = Lists.newArrayList();
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node node = jjtGetChild(i);
            if (node instanceof ASTIterableParameter) {
                aips.add((ASTIterableParameter) node);
            }
        }
        return aips;
    }

    /**
     * 构建sql与参数
     *
     * @param context
     * @return
     */
    public ParsedSql buildSqlAndArgs(RuntimeContext context) {
        StringBuffer sql = new StringBuffer();
        List<Object> args = Lists.newArrayList();
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node node = jjtGetChild(i);
            if (node instanceof ASTText) {
                ASTText text = (ASTText) node;
                sql.append(text.getText());
            } else if (node instanceof ASTNonIterableParameter) {
                ASTNonIterableParameter anip = (ASTNonIterableParameter) node;
                args.add(anip.value(context));
                sql.append("?");
            } else if (node instanceof ASTIterableParameter) {
                ASTIterableParameter aip = (ASTIterableParameter) node;
                sql.append(aip.getPropertyName()).append(" in (");
                Object objs = aip.value(context);
                int t = 0;
                for (Object obj : new Iterables(objs)) {
                    args.add(obj);
                    if (t == 0) {
                        sql.append("?");
                    } else {
                        sql.append(",?");
                    }
                    t++;
                }
                sql.append(")");
            } else if (node instanceof ASTExpression) {
                sql.append(((ASTExpression) node).value(context));
            } else {
                // TODO 合适的Exception
            }
            if (i < jjtGetNumChildren() - 1) {
                sql.append(" "); // 节点之间添加空格
            }
        }
        return new ParsedSql(sql.toString(), args.toArray());
    }

}
