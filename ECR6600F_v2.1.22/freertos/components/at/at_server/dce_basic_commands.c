#include "stdio.h"
#include "at_common.h"
#include "dce_basic_commands.h"
#include "dce_commands.h"
#include "dce.h"
#include "dce_utils.h"
#include "at_def.h"


#define DCE_FAIL printf
inline static dce_result_t  dce_handle_ATZ(dce_t* ctx, int val)
{
    // use ATZ as reset
    target_dce_reset();
    return DCE_OK;
}

inline static dce_result_t  dce_handle_ATV(dce_t* ctx, int val)
{
    if (val != 0 && val != 1)
    {
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }
    ctx->response_fmt = val;
    dce_emit_basic_result_code(ctx, DCE_RC_OK);
    return DCE_OK;

}

inline static dce_result_t  dce_handle_ATQ(dce_t* ctx, int val)
{
    if (val != 0 && val != 1)
    {
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }
    ctx->suppress_rc = val;
    dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
    return DCE_OK;
}

inline static dce_result_t  dce_handle_ATO(dce_t* ctx, int val)
{
    // TODO: figure out if we need to implement ATO
    dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
    return DCE_OK;
}

inline static dce_result_t  dce_handle_ATE(dce_t* ctx, int val)
{
    if (val != 0 && val != 1)
    {
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }
    ctx->echo = val;
    dce_emit_basic_result_code(ctx, DCE_RC_OK);
    return DCE_OK;
}

inline static dce_result_t  dce_handle_ATF(dce_t* ctx, int val)
{
    if (val != 0 && val != 1)
    {
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }

    dce_init_defaults(ctx);
    if (val == 1)
        target_dce_init_factory_defaults();

    dce_emit_basic_result_code(ctx, DCE_RC_OK);
    return DCE_OK;
}


dce_result_t  dce_process_basic_command(dce_t* ctx, const char* buf, size_t size)
{
    if (size < 1)
    {
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }
    
    char c = *buf;
    ++buf;
    --size;
    if (c == 'Z')
    {
        int val = dce_expect_number(&buf, &size, 0);
        return dce_handle_ATZ(ctx, val);
    }
    if (c == 'V')
    {
        int val = dce_expect_number(&buf, &size, 1);
        return dce_handle_ATV(ctx, val);
    }
    if (c == 'Q')
    {
        int val = dce_expect_number(&buf, &size, 0);
        return dce_handle_ATQ(ctx, val);
    }
    if (c == 'O')
    {
        int val = dce_expect_number(&buf, &size, 0);
        return dce_handle_ATO(ctx, val);
    }
    if (c == 'E')
    {
        int val = dce_expect_number(&buf, &size, -1);
        if (size > 2)
        {
            dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
            return DCE_OK; 
        }
        return dce_handle_ATE(ctx, val);
    }
    if (c == '&' && size >= 1 && buf[0] == 'F')
    {
        ++buf;
        --size;
        int val = dce_expect_number(&buf, &size, 0);
        return dce_handle_ATF(ctx, val);
    }
    if (c == '~')
    {
        os_printf(LM_APP, LL_INFO, "assert test");
    }
    dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
    return DCE_OK;
}

dce_result_t  dce_set_sparameter(dce_t* ctx, int id, int value)
{
    switch (id) {
        case 3:     // S3, 6.2.1 Command line termination character
            ctx->cr = value;
            dce_emit_basic_result_code(ctx, DCE_RC_OK);
            break;
        case 4:     // S4, 6.2.2 Response formatting character
            ctx->lf = value;
            dce_emit_basic_result_code(ctx, DCE_RC_OK);
        case 5:     // S5, 6.2.3 Command line editing character
            ctx->bs = value;
            dce_emit_basic_result_code(ctx, DCE_RC_OK);
            
        case 7:     // S7, 6.3.10 Connection completion timeout
            // TODO: implement S7 parameter writing
        default:
            dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
    }
    return DCE_OK;
}

dce_result_t  dce_get_sparameter(dce_t* ctx, int id)
{
    char text[4];
    
    switch (id) {
        case 3:     // S3, 6.2.1 Command line termination character
            dce_itoa_zeropad(ctx->cr, text, 3);
            break;
        case 4:     // S4, 6.2.2 Response formatting character
            dce_itoa_zeropad(ctx->lf, text, 3);
            break;
        case 5:     // S5, 6.2.3 Command line editing character
            dce_itoa_zeropad(ctx->bs, text, 3);
            break;
        case 7:     // S7, 6.3.10 Connection completion timeout
            // TODO: implement S7 parameter reading
        default:
            dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
            return DCE_OK;
    }
    dce_emit_information_response(ctx, text, 3);
    dce_emit_basic_result_code(ctx, DCE_RC_OK);
    return DCE_OK;
}

dce_result_t  dce_process_sparameter_command(dce_t* ctx, const char* buf, size_t size)
{
    int param_number = dce_expect_number(&buf, &size, -1);
    if (param_number == -1)
    {
        // got 'ATS', parameter number is missing
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }
    
    if (size < 1)
    {
        // got 'ATS<number>' without + or ?
        dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
        return DCE_OK;
    }
    
    char c = *buf;
    if (c == '?')
    {
        return dce_get_sparameter(ctx, param_number);
    }
    if (c == '=')
    {
        ++buf;
        --size;
        int value = dce_expect_number(&buf, &size, 0);
        return dce_set_sparameter(ctx, param_number, value);
    }
    
    dce_emit_basic_result_code(ctx, DCE_RC_ERROR);
    return DCE_OK;
}
