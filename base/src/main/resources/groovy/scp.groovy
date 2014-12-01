import net.sf.expectit.Result;
import net.sf.expectit.matcher.Matchers;
import java.util.concurrent.TimeUnit;

Result result = expect.sendLine(command).expect(Matchers.anyOf(Matchers.contains("yes/no"), Matchers.contains("password:")));
if (result.getInput().indexOf("yes/no") > 0)
	expect.sendLine("yes").expect(Matchers.contains("password:"));
expect.sendLine(password);
expect.withTimeout(timeout, TimeUnit.SECONDS).expect(Matchers.contains("100%"));